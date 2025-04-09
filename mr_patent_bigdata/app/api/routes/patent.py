import os
import uuid
import re
import pickle
import json
from fastapi import APIRouter, BackgroundTasks, HTTPException, status
from typing import Optional, List, Dict, Any
from datetime import datetime

from app.core.logging import logger
from app.core.database import database, patent, task_status
from app.schemas.patent import ProcessRequest, ProcessResponse, ResumeRequest, ResumeResponse
from app.services.extractor import extract_patent_data, prepare_corpus
from app.services.vectorizer import fit_tfidf_vectorizer
from app.services.spark_vectorizer import process_patents_with_spark  # Spark 기반 병렬 처리 함수 임포트
from striprtf.striprtf import rtf_to_text

router = APIRouter(prefix="/fastapi/patent", tags=["patent"])

# 추출된 데이터 저장을 위한 디렉토리
DATA_DIR = os.path.join(os.getcwd(), "temp_data")
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
        
        # 진행 정보를 JSON으로 저장
        progress_info = {
            "total_files": total_files,
            "processed_files": 0,
            "progress": 0
        }
        
        await database.execute(
            task_status.update().where(task_status.c.task_id == task_id).values(
                task_status="PROCESSING",
                task_error_message=json.dumps(progress_info),
                task_updated_at=datetime.utcnow()
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
                
                # 진행 정보 업데이트
                progress_info = {
                    "total_files": total_files,
                    "processed_files": processed_files,
                    "progress": progress
                }
                
                # 작업 상태 업데이트
                await database.execute(
                    task_status.update().where(task_status.c.task_id == task_id).values(
                        task_status="EXTRACTING",
                        task_error_message=json.dumps(progress_info),
                        task_updated_at=datetime.utcnow()
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
        logger.info("TF-IDF 벡터라이저 학습 완료")
        
        # 진행 정보 업데이트
        progress_info["progress"] = 60
        
        # 작업 상태 업데이트
        await database.execute(
            task_status.update().where(task_status.c.task_id == task_id).values(
                task_status="VECTORIZING",
                task_error_message=json.dumps(progress_info),
                task_updated_at=datetime.utcnow()
            )
        )
        
        # 3단계: Spark를 사용한 병렬 처리로 벡터화 수행 (with_bert=False로 설정)
        logger.info(f"총 {len(all_patents)}개 특허 Spark 병렬 벡터화 시작")
        total_processed = await process_patents_with_spark(all_patents, batch_size=1000, with_bert=False)
        
        # 진행 정보 업데이트
        progress_info["progress"] = 100
        
        # 작업 완료
        await database.execute(
            task_status.update().where(task_status.c.task_id == task_id).values(
                task_status="COMPLETED",
                task_error_message=json.dumps(progress_info),
                task_updated_at=datetime.utcnow()
            )
        )
        
        logger.info(f"특허 데이터 처리 완료: {total_processed}개의 특허 저장")
        
    except Exception as e:
        logger.error(f"데이터 처리 중 오류 발생: {e}")
        await database.execute(
            task_status.update().where(task_status.c.task_id == task_id).values(
                task_status="FAILED",
                task_error_message=str(e),
                task_updated_at=datetime.utcnow()
            )
        )
        raise

async def process_saved_data(task_id: str, filename: str):
    """저장된 특허 데이터를 벡터화하고 데이터베이스에 저장 (Spark 병렬 처리 적용)"""
    try:        
        # 저장된 데이터 로드
        all_patents = load_extracted_data(filename)

        # 출원번호에서 "발명의명칭" 제거
        for patent_data in all_patents:
            if "application_number" in patent_data and "발명의명칭" in patent_data["application_number"]:
                patent_data["application_number"] = patent_data["application_number"].replace("발명의명칭", "")
        
        # 진행 정보 초기화
        progress_info = {
            "total_patents": len(all_patents),
            "progress": 30
        }
        
        # TF-IDF 벡터라이저 학습
        logger.info("TF-IDF 벡터라이저 학습 시작")
        corpus = prepare_corpus(all_patents)
        fit_tfidf_vectorizer(corpus)
        logger.info("TF-IDF 벡터라이저 학습 완료")
        
        # 진행 정보 업데이트
        progress_info["progress"] = 60
        
        # 작업 상태 업데이트
        await database.execute(
            task_status.update().where(task_status.c.task_id == task_id).values(
                task_status="VECTORIZING",
                task_error_message=json.dumps(progress_info),
                task_updated_at=datetime.utcnow()
            )
        )
        
        # Spark로 병렬 처리 수행 (with_bert=False, batch_size=1000으로 설정)
        logger.info(f"총 {len(all_patents)}개 특허 Spark 병렬 벡터화 시작")
        total_processed = await process_patents_with_spark(all_patents, batch_size=1000, with_bert=False)
        
        # 진행 정보 업데이트
        progress_info["progress"] = 100
        progress_info["processed_patents"] = total_processed
        
        # 작업 완료
        await database.execute(
            task_status.update().where(task_status.c.task_id == task_id).values(
                task_status="COMPLETED",
                task_error_message=json.dumps(progress_info),
                task_updated_at=datetime.utcnow()
            )
        )
        
        logger.info(f"저장된 특허 데이터 처리 완료: {total_processed}개 특허 저장")
        
    except Exception as e:
        logger.error(f"저장된 데이터 처리 중 오류 발생: {e}")
        await database.execute(
            task_status.update().where(task_status.c.task_id == task_id).values(
                task_status="FAILED",
                task_error_message=str(e),
                task_updated_at=datetime.utcnow()
            )
        )

# 추가된 엔드포인트 - 특허 파일 처리 시작
@router.post("/extract-and-vectorize", response_model=ProcessResponse)
async def extract_and_vectorize(request: ProcessRequest, background_tasks: BackgroundTasks):
    """특허 RTF 파일 처리 시작"""
    task_id = str(uuid.uuid4())
    now = datetime.utcnow()
    
    # 작업 상태 데이터베이스에 초기 상태 저장 (새 스키마 적용)
    await database.execute(
        task_status.insert().values(
            task_id=task_id,
            patent_draft_id=None,  # 특허 파일 처리는 특정 초안과 관련 없음
            task_type="extract_vectorize",
            task_status="PENDING",
            task_error_message=None,
            task_created_at=now,
            task_updated_at=now
        )
    )
    
    # 백그라운드에서 처리 시작
    background_tasks.add_task(process_patent_files, request.rtf_directory, task_id)
    
    return ProcessResponse(
        task_id=task_id,
        status=True,
        message="특허 데이터 처리가 시작되었습니다.",
        directory=request.rtf_directory
    )

# 추가된 엔드포인트 - 저장된 데이터에서 처리 재개
@router.post("/resume-from-saved", response_model=ResumeResponse)
async def resume_from_saved(request: ResumeRequest, background_tasks: BackgroundTasks):
    """저장된 데이터에서 처리 재개"""
    task_id = str(uuid.uuid4())
    now = datetime.utcnow()
    
    # 작업 상태 데이터베이스에 초기 상태 저장
    await database.execute(
        task_status.insert().values(
            task_id=task_id,
            patent_draft_id=0,  # 특허 파일 처리는 특정 초안과 관련 없음
            task_type="resume_processing",
            task_status="PENDING",
            task_error_message=None,
            task_created_at=now,
            task_updated_at=now
        )
    )
    
    # 백그라운드에서 처리 시작
    background_tasks.add_task(process_saved_data, task_id, request.filename)
    
    return ResumeResponse(task_id=task_id)

# 추가된 엔드포인트 - 실패한 작업 재시도
@router.post("/resume-processing", response_model=ResumeResponse)
async def resume_processing(request: ResumeRequest, background_tasks: BackgroundTasks):
    """이전에 실패한 작업 재시도"""
    # 기존 작업 정보 확인
    task_query = """
    SELECT * FROM task_status WHERE task_id = :task_id
    """
    old_task = await database.fetch_one(
        query=task_query,
        values={"task_id": request.task_id}
    )
    
    if not old_task:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"작업 ID {request.task_id}를 찾을 수 없습니다."
        )
    
    # 새 작업 ID 생성
    new_task_id = str(uuid.uuid4())
    now = datetime.utcnow()
    
    # 작업 상태 데이터베이스에 초기 상태 저장
    await database.execute(
        task_status.insert().values(
            task_id=new_task_id,
            patent_draft_id=0,  # 특허 파일 처리는 특정 초안과 관련 없음
            task_type="retry_processing",
            task_status="PENDING",
            task_error_message=None,
            task_created_at=now,
            task_updated_at=now
        )
    )
    
    # 저장된 파일명 확인
    if not request.filename:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="재처리를 위한 파일명이 필요합니다."
        )
    
    # 백그라운드에서 처리 시작
    background_tasks.add_task(process_saved_data, new_task_id, request.filename)
    
    return ResumeResponse(task_id=new_task_id)
