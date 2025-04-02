import os
import uuid
import re
import pickle
from fastapi import APIRouter, BackgroundTasks, HTTPException, status
from typing import Optional, List, Dict, Any
from datetime import datetime

from app.core.logging import logger
from app.core.database import database, patent, task_status
from app.schemas.patent import ProcessRequest, ProcessResponse, ResumeRequest, ResumeResponse
from app.services.extractor import extract_patent_data, prepare_corpus
from app.services.vectorizer import fit_tfidf_vectorizer, generate_vectors
from striprtf.striprtf import rtf_to_text

router = APIRouter(prefix="/api/patents", tags=["patents"])

# 추출된 데이터 저장을 위한 디렉토리
DATA_DIR = "temp_data"
os.makedirs(DATA_DIR, exist_ok=True)

def save_extracted_data(patents: List[Dict[str, Any]], filename: str = "extracted_patents.pkl") -> str:
    """추출된 특허 데이터를 파일로 저장"""
    save_path = os.path.join(DATA_DIR, filename)
    with open(save_path, 'wb') as f:
        pickle.dump(patents, f)
    logger.info(f"추출된 특허 데이터 {len(patents)}개를 {save_path}에 저장")
    return save_path

def load_extracted_data(filename: str = "extracted_patents.pkl") -> List[Dict[str, Any]]:
    """저장된 특허 데이터 로드"""
    load_path = os.path.join(DATA_DIR, filename)
    if not os.path.exists(load_path):
        raise FileNotFoundError(f"저장된 특허 데이터를 찾을 수 없습니다: {load_path}")
    
    with open(load_path, 'rb') as f:
        patents = pickle.load(f)
    logger.info(f"저장된 특허 데이터 {len(patents)}개 로드 완료")
    return patents

async def process_patent_files(rtf_directory: str, task_id: str):
    """특허 데이터 처리 및 저장"""
    try:        
        # 작업 상태 초기화
        rtf_files = [f for f in os.listdir(rtf_directory) if f.endswith(".rtf")]
        total_files = len(rtf_files)
        
        await database.execute(
            task_status.update().where(task_status.c.task_id == task_id).values(
                total_files=total_files,
                processed_files=0,
                progress=0
            )
        )
        
        # 파일명 패턴
        filename_pattern = r"([a-zA-Z]\d+)_(\d+)_(\d+)\.rtf"
        
        all_patents = []
        processed_files = 0
        
        # 1단계: 모든 파일에서 특허 데이터 추출
        for filename in rtf_files:
            ipc_match = re.match(filename_pattern, filename)
            ipc_code = ipc_match.group(1) if ipc_match else None
            
            file_path = os.path.join(rtf_directory, filename)
            try:
                with open(file_path, 'r', encoding='utf-8') as file:
                    rtf_content = file.read()
                rtf_text = rtf_to_text(rtf_content)
                logger.info(f"RTF 변환 결과 (처음 500자): {rtf_text[:500]}")
                
                # 특허 데이터 추출
                patents = extract_patent_data(rtf_text)
                
                # IPC 코드 추가
                for patent_data in patents:
                    patent_data['ipc_from_filename'] = ipc_code
                
                all_patents.extend(patents)
                
                processed_files += 1
                progress = int((processed_files / total_files) * 50)
                
                # 작업 상태 업데이트
                await database.execute(
                    task_status.update().where(task_status.c.task_id == task_id).values(
                        processed_files=processed_files,
                        progress=progress,
                        status="EXTRACTING"
                    )
                )
                
                logger.info(f"파일 {filename} 처리 완료: {len(patents)}개 특허 추출")
                
            except Exception as e:
                logger.error(f"Error processing {filename}: {e}")
        
        # 추출된 데이터 저장 (나중에 재개할 수 있도록)
        save_filename = f"patents_{datetime.now().strftime('%Y%m%d_%H%M%S')}.pkl"
        save_path = save_extracted_data(all_patents, save_filename)
        
        # 2단계: TF-IDF 벡터라이저 학습
        logger.info("TF-IDF 벡터라이저 학습 시작")
        corpus = prepare_corpus(all_patents)
        fit_tfidf_vectorizer(corpus)
        
        # 작업 상태 업데이트
        await database.execute(
            task_status.update().where(task_status.c.task_id == task_id).values(
                progress=60,
                status="VECTORIZING"
            )
        )
        
        # 3단계: 벡터화 및 저장
        total_patents = len(all_patents)
        logger.info(f"총 {total_patents}개 특허 벡터화 및 저장 시작")
        
        for i, patent_data in enumerate(all_patents):
            try:
                # 벡터 생성
                tfidf_vector, kobert_vector = generate_vectors(patent_data)
                
                # IPC 코드 길이 제한 (오류 방지)
                ipc_code = patent_data.get("ipc_classification", "")
                if ipc_code and len(ipc_code) > 95:
                    ipc_code = ipc_code[:95]
                
                # 데이터베이스에 저장 - patent_created_at, patent_updated_at 추가
                query = patent.insert().values(
                    patent_title=patent_data.get("title"),
                    patent_application_number=patent_data.get("application_number"),
                    patent_ipc=ipc_code,
                    patent_summary=patent_data.get("summary"),
                    patent_claim=patent_data.get("claims"),
                    patent_tfidf_vector=tfidf_vector.tobytes(),
                    patent_kobert_vector=kobert_vector.tobytes(),
                    patent_created_at=datetime.utcnow(),
                    patent_updated_at=datetime.utcnow()
                )
                
                await database.execute(query)
                
                # 작업 상태 업데이트
                if i % 100 == 0 or i == total_patents - 1:
                    progress = 60 + int((i / total_patents) * 40)
                    await database.execute(
                        task_status.update().where(task_status.c.task_id == task_id).values(
                            progress=progress,
                            status="VECTORIZING"
                        )
                    )
                    logger.info(f"진행률: {progress}%, {i+1}/{total_patents} 처리 완료")
            except Exception as e:
                logger.error(f"데이터 저장 중 오류 발생: {e}, 특허 번호: {patent_data.get('application_number')}")
                # 개별 특허에서 오류 발생시 건너뛰고 다음 특허 처리
                continue
        
        # 작업 완료
        await database.execute(
            task_status.update().where(task_status.c.task_id == task_id).values(
                progress=100,
                status="COMPLETED"
            )
        )
        
        logger.info(f"특허 데이터 처리 완료: {total_patents}개의 특허 저장")
        
    except Exception as e:
        logger.error(f"데이터 처리 중 오류 발생: {e}")
        await database.execute(
            task_status.update().where(task_status.c.task_id == task_id).values(
                status="FAILED"
            )
        )
        raise

async def resume_data_processing(rtf_directory: str, task_id: str, start_index: int = 0):
    """벡터화된 특허 데이터의 저장 작업만 재개합니다."""
    try:        
        # 작업 상태 초기화
        rtf_files = [f for f in os.listdir(rtf_directory) if f.endswith(".rtf")]
        total_files = len(rtf_files)
        
        await database.execute(
            task_status.update().where(task_status.c.task_id == task_id).values(
                total_files=total_files,
                progress=60  # 벡터화까지 완료된 상태를 나타냄
            )
        )
        
        # 데이터 추출 및 벡터화 (이미 완료된 것으로 가정하고 빠르게 처리)
        all_patents = []
        
        for filename in rtf_files:
            file_path = os.path.join(rtf_directory, filename)
            try:
                with open(file_path, 'r', encoding='utf-8') as file:
                    rtf_content = file.read()
                rtf_text = rtf_to_text(rtf_content)
                
                # 특허 데이터 추출
                patents = extract_patent_data(rtf_text)
                all_patents.extend(patents)
                
                logger.info(f"파일 {filename} 로드 완료: {len(patents)}개 특허")
                
            except Exception as e:
                logger.error(f"Error loading {filename}: {e}")
        
        # TF-IDF 벡터라이저 재학습 (필요한 경우)
        logger.info("TF-IDF 벡터라이저 학습 시작")
        corpus = prepare_corpus(all_patents)
        fit_tfidf_vectorizer(corpus)
        
        # 저장만 재개 (시작 인덱스부터)
        total_patents = len(all_patents)
        logger.info(f"총 {total_patents}개 특허 중 {start_index}번째부터 저장 시작")
        
        for i in range(start_index, total_patents):
            try:
                patent_data = all_patents[i]
                
                # 벡터 생성
                tfidf_vector, kobert_vector = generate_vectors(patent_data)
                
                # IPC 코드 길이 제한 (오류 방지)
                ipc_code = patent_data.get("ipc_classification", "")
                if ipc_code and len(ipc_code) > 95:
                    ipc_code = ipc_code[:95]
                
                # 데이터베이스에 저장 - patent_created_at, patent_updated_at 추가
                query = patent.insert().values(
                    patent_title=patent_data.get("title"),
                    patent_application_number=patent_data.get("application_number"),
                    patent_ipc=ipc_code,
                    patent_summary=patent_data.get("summary"),
                    patent_claim=patent_data.get("claims"),
                    patent_tfidf_vector=tfidf_vector.tobytes(),
                    patent_kobert_vector=kobert_vector.tobytes(),
                    patent_created_at=datetime.utcnow(),
                    patent_updated_at=datetime.utcnow()
                )
                
                await database.execute(query)
                
                # 작업 상태 업데이트
                if i % 100 == 0 or i == total_patents - 1:
                    progress = 60 + int(((i - start_index) / (total_patents - start_index)) * 40)
                    await database.execute(
                        task_status.update().where(task_status.c.task_id == task_id).values(
                            progress=progress,
                            status="RESUMING_SAVE"
                        )
                    )
                    logger.info(f"진행률: {progress}%, {i+1}/{total_patents} 처리 완료")
            except Exception as e:
                logger.error(f"데이터 저장 중 오류 발생: {e}, 특허 번호: {patent_data.get('application_number')}")
                # 개별 특허에서 오류 발생시 건너뛰고 다음 특허 처리
                continue
        
        # 작업 완료
        await database.execute(
            task_status.update().where(task_status.c.task_id == task_id).values(
                progress=100,
                status="COMPLETED"
            )
        )
        
        logger.info(f"특허 데이터 저장 재개 완료: {total_patents - start_index}개의 특허 저장")
        
    except Exception as e:
        logger.error(f"데이터 저장 재개 중 오류 발생: {e}")
        await database.execute(
            task_status.update().where(task_status.c.task_id == task_id).values(
                status="FAILED"
            )
        )
        raise

async def process_saved_data(task_id: str, filename: str):
    """저장된 특허 데이터를 벡터화하고 데이터베이스에 저장"""
    try:        
        # 저장된 데이터 로드
        all_patents = load_extracted_data(filename)
        
        # TF-IDF 벡터라이저 학습
        logger.info("TF-IDF 벡터라이저 학습 시작")
        corpus = prepare_corpus(all_patents)
        fit_tfidf_vectorizer(corpus)
        
        # 작업 상태 업데이트
        await database.execute(
            task_status.update().where(task_status.c.task_id == task_id).values(
                progress=60,
                status="VECTORIZING"
            )
        )
        
        # 벡터화 및 저장
        total_patents = len(all_patents)
        logger.info(f"총 {total_patents}개 특허 벡터화 및 저장 시작")
        
        for i, patent_data in enumerate(all_patents):
            try:
                # 벡터 생성
                tfidf_vector, kobert_vector = generate_vectors(patent_data)
                
                # IPC 코드 길이 제한
                ipc_code = patent_data.get("ipc_classification", "")
                if ipc_code and len(ipc_code) > 95:
                    ipc_code = ipc_code[:95]
                
                # 데이터베이스에 저장
                query = patent.insert().values(
                    patent_title=patent_data.get("title"),
                    patent_application_number=patent_data.get("application_number"),
                    patent_ipc=ipc_code,
                    patent_summary=patent_data.get("summary"),
                    patent_claim=patent_data.get("claims"),
                    patent_tfidf_vector=tfidf_vector.tobytes(),
                    patent_kobert_vector=kobert_vector.tobytes(),
                    patent_created_at=datetime.utcnow(),
                    patent_updated_at=datetime.utcnow()
                )
                
                await database.execute(query)
                
                # 작업 상태 업데이트
                if i % 100 == 0 or i == total_patents - 1:
                    progress = 60 + int((i / total_patents) * 40)
                    await database.execute(
                        task_status.update().where(task_status.c.task_id == task_id).values(
                            progress=progress,
                            status="VECTORIZING"
                        )
                    )
                    logger.info(f"진행률: {progress}%, {i+1}/{total_patents} 처리 완료")
            except Exception as e:
                logger.error(f"데이터 저장 중 오류 발생: {e}, 특허 번호: {patent_data.get('application_number')}")
                # 개별 특허에서 오류 발생시 건너뛰고 다음 특허 처리
                continue
        
        # 작업 완료
        await database.execute(
            task_status.update().where(task_status.c.task_id == task_id).values(
                progress=100,
                status="COMPLETED"
            )
        )
        
        logger.info(f"저장된 특허 데이터 처리 완료: {total_patents}개의 특허 저장")
        
    except Exception as e:
        logger.error(f"저장된 데이터 처리 중 오류 발생: {e}")
        await database.execute(
            task_status.update().where(task_status.c.task_id == task_id).values(
                status="FAILED"
            )
        )
        raise

@router.post("/extract-and-vectorize", response_model=ProcessResponse)
async def extract_and_vectorize(request: ProcessRequest, background_tasks: BackgroundTasks):
    """특허 RTF 파일에서 데이터 추출 및 벡터화 수행"""
    if not os.path.exists(request.rtf_directory):
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"디렉토리를 찾을 수 없습니다: {request.rtf_directory}"
        )
    
    # 폴더 내 RTF 파일 존재 여부 확인
    rtf_files = [f for f in os.listdir(request.rtf_directory) if f.endswith(".rtf")]
    if not rtf_files:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="지정한 디렉토리에 RTF 파일이 없습니다."
        )
    
    # 작업 ID 생성
    task_id = f"task_{uuid.uuid4().hex[:10]}"
    
    # 작업 상태 저장
    await database.execute(
        task_status.insert().values(
            task_id=task_id,
            status="STARTED",
            progress=0,
            total_files=0,
            processed_files=0
        )
    )
    
    # 백그라운드 작업 시작
    background_tasks.add_task(process_patent_files, request.rtf_directory, task_id)
    
    return {
        "status": True,
        "message": "특허 데이터 추출 및 벡터화 작업이 백그라운드에서 시작되었습니다.",
        "task_id": task_id,
        "directory": request.rtf_directory
    }

@router.post("/resume-processing", response_model=ResumeResponse)
async def resume_processing(request: ResumeRequest, background_tasks: BackgroundTasks):
    """이전에 중단된 특허 데이터 처리를 재개합니다."""
    if not os.path.exists(request.rtf_directory):
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"디렉토리를 찾을 수 없습니다: {request.rtf_directory}"
        )
    
    # 폴더 내 RTF 파일 존재 여부 확인
    rtf_files = [f for f in os.listdir(request.rtf_directory) if f.endswith(".rtf")]
    if not rtf_files:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="지정한 디렉토리에 RTF 파일이 없습니다."
        )
    
    # 작업 ID 생성
    task_id = f"resume_{uuid.uuid4().hex[:10]}"
    
    # 작업 상태 저장
    await database.execute(
        task_status.insert().values(
            task_id=task_id,
            status="RESUMED",
            progress=50,  # 벡터화까지 이미 진행되었다고 가정
            total_files=len(rtf_files),
            processed_files=len(rtf_files)  # 모든 파일이 처리된 것으로 표시
        )
    )
    
    # 백그라운드 작업 시작 (저장 작업부터 재개)
    background_tasks.add_task(resume_data_processing, request.rtf_directory, task_id, request.start_index)
    
    return {
        "status": True,
        "message": "특허 데이터 저장 작업이 백그라운드에서 재개되었습니다.",
        "task_id": task_id,
        "directory": request.rtf_directory
    }

@router.post("/resume-from-saved", response_model=ProcessResponse)
async def resume_from_saved(request: Dict[str, str], background_tasks: BackgroundTasks):
    """저장된 특허 데이터를 로드하여 벡터화 및 저장 작업만 수행합니다."""
    filename = request.get("filename", "extracted_patents.pkl")
    
    # 저장된 파일 존재 여부 확인
    file_path = os.path.join(DATA_DIR, filename)
    if not os.path.exists(file_path):
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"저장된 특허 데이터 파일을 찾을 수 없습니다: {file_path}"
        )
    
    # 작업 ID 생성
    task_id = f"saved_{uuid.uuid4().hex[:10]}"
    
    # 작업 상태 저장
    await database.execute(
        task_status.insert().values(
            task_id=task_id,
            status="PROCESSING_SAVED",
            progress=50,  # 데이터 추출은 이미 완료됨
            total_files=1,
            processed_files=1
        )
    )
    
    # 백그라운드 작업 시작 (저장된 데이터 처리)
    background_tasks.add_task(process_saved_data, task_id, filename)
    
    return {
        "status": True,
        "message": "저장된 특허 데이터 처리가 백그라운드에서 시작되었습니다.",
        "task_id": task_id,
        "directory": DATA_DIR
    }
