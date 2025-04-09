from fastapi import APIRouter, HTTPException, BackgroundTasks
from typing import Dict, Any, List
from datetime import datetime, timezone
import numpy as np
from sklearn.metrics.pairwise import cosine_similarity
import json
import re
import traceback
import logging
import httpx
import asyncio

from app.core.database import database
from app.services.vectorizer import get_tfidf_vector, get_bert_vector
from app.services.kipris import get_patent_public_info, download_patent_pdf

router = APIRouter(prefix="/fastapi", tags=["similarity"])

logger = logging.getLogger(__name__)

def get_current_timestamp():
    """현재 시간을 ISO 8601 형식으로 변환 (UTC)"""
    return datetime.now(timezone.utc).isoformat().replace('+00:00', 'Z')

# 섹션 처리 도우미 함수
async def process_section(section_key, section_text, patent_text):
    if not section_text or len(section_text.strip()) < 10:
        return None
        
    # KLUE-BERT 모델을 활용한 문맥 비교
    section_vector = get_bert_vector(section_text)
    
    # 공고전문을 600자 단위로 분할하여 비교
    best_match = {
        "text": "",
        "similarity": 0.0
    }
    
    # 청크 크기 최적화: 300자→600자, 이동 간격: 150자→300자
    for i in range(0, len(patent_text), 300):
        chunk = patent_text[i:i+600]
        if len(chunk) < 50:  # 너무 짧은 청크는 건너뛰기
            continue
            
        chunk_vector = get_bert_vector(chunk)
        similarity = safe_cosine_similarity(section_vector, chunk_vector)
        
        if similarity > best_match["similarity"]:
            best_match["text"] = chunk
            best_match["similarity"] = similarity
    
    # 유사도가 0.6 이상인 경우만 반환
    if best_match["similarity"] >= 0.6:
        return {
            "user_section": section_key,
            "patent_section": "CONTENT",
            "user_text": section_text[:200],
            "patent_text": best_match["text"][:200],
            "similarity_score": best_match["similarity"]
        }
    return None

# 벡터 차원 불일치 문제와 NaN 문제를 해결하는 안전한 변환 함수
def safe_frombuffer(buffer, target_dim=1000, dtype=np.float32):
    """안전하게 버퍼를 배열로 변환하고 차원을 맞춰주는 함수"""
    if buffer is None or len(buffer) == 0:
        return np.zeros(target_dim, dtype=dtype)
    
    try:
        # 버퍼를 배열로 변환
        vec = np.frombuffer(buffer, dtype=dtype)
        
        # NaN 값 처리 - 추가된 부분
        if np.isnan(vec).any():
            vec = np.nan_to_num(vec, nan=0.0)
        
        # 차원 불일치 해결
        if len(vec) > target_dim:
            # 더 큰 경우 잘라내기
            return vec[:target_dim]
        elif len(vec) < target_dim:
            # 더 작은 경우 제로 패딩
            result = np.zeros(target_dim, dtype=dtype)
            result[:len(vec)] = vec
            return result
        return vec
    except ValueError:
        # 버퍼 크기 오류 등 예외 발생 시 영벡터 반환
        return np.zeros(target_dim, dtype=dtype)

# 안전한 코사인 유사도 계산 함수
def safe_cosine_similarity(a, b):
    """NaN을 처리하는 안전한 코사인 유사도 계산 함수"""
    # NaN 값 확인 및 처리
    a = np.nan_to_num(a, nan=0.0)
    b = np.nan_to_num(b, nan=0.0)
    
    # 벡터가 모두 0인 경우 처리
    if np.linalg.norm(a) == 0 or np.linalg.norm(b) == 0:
        return 0.0
    
    try:
        return float(cosine_similarity([a], [b])[0][0])
    except Exception:
        # 기타 오류 발생 시 0 반환
        return 0.0

# FCM 알림 전송 함수 추가
async def send_fcm_notification(user_id: str, title: str, body: str, data: Dict = None):
    """FCM을 통해 알림을 전송합니다."""
    try:
        # FCM 서버 URL (Spring 서버 API)
        fcm_url = "http://localhost:8080/api/fcm/token/python"
        
        # FCM 메시지 구성
        fcm_message = {
            "userId": user_id,
            "title": title,
            "body": body,
            "data": data or {}
        }
        
        # Spring 백엔드로 FCM 요청 전송
        async with httpx.AsyncClient() as client:
            response = await client.post(fcm_url, json=fcm_message, timeout=10.0)
            
            if response.status_code == 200:
                logger.info(f"FCM 알림 전송 성공: {user_id}")
                return True
            else:
                logger.error(f"FCM 알림 전송 실패: 상태 코드 {response.status_code}")
                return False
                
    except Exception as e:
        logger.error(f"FCM 알림 전송 중 오류: {str(e)}")
        return False

# 사용자 ID 조회 함수
async def get_draft_owner(patent_draft_id: int) -> str:
    """특허 초안 소유자의 사용자 ID를 조회합니다."""
    query = """
    SELECT pd.user_patent_folder_id, upf.user_id 
    FROM patent_draft pd
    JOIN user_patent_folder upf ON pd.user_patent_folder_id = upf.user_patent_folder_id
    WHERE pd.patent_draft_id = :patent_draft_id
    """
    
    result = await database.fetch_one(
        query=query,
        values={"patent_draft_id": patent_draft_id}
    )
    
    # return result["user_id"] if result else None
    return 1 # 테스트 목적으로 기본값 사용

# 특허 초안 제목 조회 함수
async def get_draft_title(patent_draft_id: int) -> str:
    """특허 초안의 제목을 조회합니다."""
    query = """
    SELECT patent_draft_title FROM patent_draft 
    WHERE patent_draft_id = :draft_id
    """
    
    result = await database.fetch_one(
        query=query,
        values={"draft_id": patent_draft_id}  # 이 부분 수정: patent_draft_id → draft_id
    )
    
    return result["patent_draft_title"] if result else "특허 초안"

# 비동기 처리를 위한 작업 상태 관리 함수들
async def create_similarity_task(patent_draft_id: int) -> str:
    """유사도 검사 작업 상태 레코드를 생성합니다."""
    now = datetime.now(timezone.utc)
    task_id = f"task_{patent_draft_id}_{int(now.timestamp())}"
    
    query = """
    INSERT INTO task_status (
        task_id, patent_draft_id, task_type, task_status, 
        task_created_at, task_updated_at
    ) VALUES (
        :task_id, :patent_draft_id, 'similarity_check', 'started',
        :created_at, :updated_at
    )
    """
    
    await database.execute(
        query=query,
        values={
            "task_id": task_id,
            "patent_draft_id": patent_draft_id,
            "created_at": now,
            "updated_at": now
        }
    )
    
    return task_id

async def update_task_status(task_id: str, status: str, error_message: str = None):
    """작업 상태를 업데이트합니다."""
    now = datetime.now(timezone.utc)
    
    query = """
    UPDATE task_status 
    SET task_status = :status, 
        task_error_message = :error_message,
        task_updated_at = :updated_at
    WHERE task_id = :task_id
    """
    
    await database.execute(
        query=query,
        values={
            "task_id": task_id,
            "status": status,
            "error_message": error_message,
            "updated_at": now
        }
    )

@router.post("/draft/{patent_draft_id}/similarity-check", response_model=Dict[str, Any])
async def run_similarity_check(patent_draft_id: int, background_tasks: BackgroundTasks):
    """특허 초안의 적합도 검사, 유사도 분석, 상세 비교를 비동기적으로 수행합니다."""
    try:
        # 특허 초안 존재 확인
        draft_query = """
        SELECT * FROM patent_draft 
        WHERE patent_draft_id = :draft_id
        """
        draft = await database.fetch_one(
            query=draft_query,
            values={"draft_id": patent_draft_id}
        )
        
        if not draft:
            raise HTTPException(
                status_code=404,
                detail={
                    "code": "DRAFT_NOT_FOUND",
                    "message": "해당 ID의 특허 초안을 찾을 수 없습니다."
                }
            )
        
        # 작업 상태 생성
        task_id = await create_similarity_task(patent_draft_id)
        
        # 백그라운드 작업으로 유사도 검사 수행
        background_tasks.add_task(
            process_similarity_check,
            patent_draft_id=patent_draft_id,
            task_id=task_id
        )
        
        return {
            "data": {
                "task_id": task_id,
                "status": "processing",
                "message": "유사도 검사가 시작되었습니다. 완료 시 알림이 전송됩니다."
            }
        }
    
    except HTTPException:
        raise
    
    except Exception as e:
        logger.error(f"유사도 검사 시작 중 오류 발생: {str(e)}")
        logger.error(traceback.format_exc())
        
        raise HTTPException(
            status_code=500,
            detail={
                "code": "SIMILARITY_ERROR",
                "message": f"유사도 검사 시작 중 오류 발생: {str(e)}"
            }
        )

# 백그라운드 작업 처리 함수 추가
async def process_similarity_check(patent_draft_id: int, task_id: str):
    """백그라운드에서 유사도 검사 전체 프로세스를 수행합니다."""
    try:
        # 특허 초안 정보 조회
        draft_query = """
        SELECT * FROM patent_draft 
        WHERE patent_draft_id = :draft_id
        """
        draft = await database.fetch_one(
            query=draft_query,
            values={"draft_id": patent_draft_id}
        )
        
        if not draft:
            await update_task_status(task_id, "failed", "특허 초안을 찾을 수 없습니다.")
            return
        
        # 1. 적합도 검사 수행
        logger.info(f"적합도 검사 시작: 특허 초안 ID {patent_draft_id}")
        await update_task_status(task_id, "fitness_check")
        now = datetime.now(timezone.utc)
        fitness_results = await perform_fitness_check(patent_draft_id, draft, now)
        await update_task_status(task_id, "fitness_completed")
        
        # 2. KNN 기반 유사도 검색 수행
        logger.info(f"유사도 검색 시작: 특허 초안 ID {patent_draft_id}")
        await update_task_status(task_id, "similarity_search")
        from app.services.knn_search import perform_knn_search
        similarity_result = await perform_knn_search(patent_draft_id, k=20)
        
        if not similarity_result:
            await update_task_status(task_id, "failed", "유사도 검색에 실패했습니다.")
            
            # 실패 알림 전송
            user_id = await get_draft_owner(patent_draft_id)
            if user_id:
                await send_fcm_notification(
                    user_id=user_id,
                    title="유사도 검사 실패",
                    body="특허 초안의 유사도 검사 중 오류가 발생했습니다.",
                    data={
                        "patent_draft_id": patent_draft_id,
                        "error": "similarity_search_failed"
                    }
                )
            return
        
        await update_task_status(task_id, "similarity_completed")
        
        # 3. 상세 비교 수행 (상위 1개 특허만)
        logger.info(f"상세 비교 시작: 특허 초안 ID {patent_draft_id}")
        await update_task_status(task_id, "detailed_comparison")
        top_results = similarity_result["results"][:1]
        
        if top_results:
            await perform_detailed_comparison(patent_draft_id, draft, top_results, now)
        await update_task_status(task_id, "completed")
        
        # 4. FCM 알림 전송
        user_id = await get_draft_owner(patent_draft_id)
        draft_title = await get_draft_title(patent_draft_id)
        
        if user_id:
            await send_fcm_notification(
                user_id=user_id,
                title="유사도 검사 완료",
                body=f"'{draft_title}' 특허 초안의 유사도 검사가 완료되었습니다.",
                data={
                    "patent_draft_id": patent_draft_id,
                    "similarity_id": similarity_result["similarity_id"],
                    "notification_type": "similarity_completed"
                }
            )
        
        logger.info(f"유사도 검사 전체 프로세스 완료: 특허 초안 ID {patent_draft_id}")
        
    except Exception as e:
        logger.error(f"유사도 검사 처리 중 오류: {str(e)}")
        logger.error(traceback.format_exc())
        await update_task_status(task_id, "failed", str(e))
        
        # 오류 발생 시에도 FCM 알림
        user_id = await get_draft_owner(patent_draft_id)
        if user_id:
            await send_fcm_notification(
                user_id=user_id,
                title="유사도 검사 실패",
                body="특허 초안 검사 중 오류가 발생했습니다.",
                data={
                    "patent_draft_id": patent_draft_id,
                    "error": "process_failed"
                }
            )

# 작업 상태 조회 API 추가
@router.get("/task/{task_id}/status", response_model=Dict[str, Any])
async def get_task_status(task_id: str):
    """작업 상태를 조회합니다."""
    query = """
    SELECT * FROM task_status
    WHERE task_id = :task_id
    """
    
    result = await database.fetch_one(
        query=query,
        values={"task_id": task_id}
    )
    
    if not result:
        raise HTTPException(
            status_code=404,
            detail={
                "code": "TASK_NOT_FOUND",
                "message": "해당 작업을 찾을 수 없습니다."
            }
        )
    
    return {
        "data": {
            "task_id": result["task_id"],
            "patent_draft_id": result["patent_draft_id"],
            "task_type": result["task_type"],
            "task_status": result["task_status"],
            "error_message": result["task_error_message"],
            "created_at": result["task_created_at"],
            "updated_at": result["task_updated_at"]
        }
    }

# 새로 추가: 적합도 결과 조회 API
@router.get("/patent/fitness/{patent_draft_id}", response_model=Dict[str, Any])
async def get_fitness_result(patent_draft_id: int):
    """특허 초안의 적합도 검사 결과 조회"""
    # 데이터베이스에서 적합도 결과 조회
    query = """
    SELECT * FROM fitness
    WHERE patent_draft_id = :patent_draft_id
    ORDER BY fitness_created_at DESC
    LIMIT 1
    """
    
    fitness = await database.fetch_one(
        query=query,
        values={"patent_draft_id": patent_draft_id}
    )
    
    if not fitness:
        raise HTTPException(
            status_code=404,
            detail={
                "code": "FITNESS_NOT_FOUND",
                "message": "해당 특허 초안의 적합도 검사 결과가 없습니다."
            }
        )
    
    # Record 객체를 딕셔너리로 변환
    fitness_dict = dict(fitness)
    
    # fitness_good_content가 JSON 문자열인 경우 파싱
    fitness_good_content = fitness_dict.get("fitness_good_content")
    if fitness_good_content and isinstance(fitness_good_content, str):
        try:
            fitness_good_content = json.loads(fitness_good_content)
        except json.JSONDecodeError:
            fitness_good_content = {}
    
    return {
        "data": {
            "fitness_id": fitness_dict["fitness_id"],
            "patent_draft_id": fitness_dict["patent_draft_id"],
            "is_corrected": fitness_dict["fitness_is_corrected"],
            "details": fitness_good_content,
            "created_at": fitness_dict["fitness_created_at"]
        }
    }

# 새로 추가: 유사도 결과 조회 API
@router.get("/patent/similarity/{patent_draft_id}", response_model=Dict[str, Any])
async def get_similarity_result(patent_draft_id: int):
    """특허 초안의 유사도 검사 결과 조회"""
    # 특허 초안 정보 조회
    draft_query = """
    SELECT * FROM patent_draft 
    WHERE patent_draft_id = :draft_id
    """
    
    draft = await database.fetch_one(
        query=draft_query,
        values={"draft_id": patent_draft_id}
    )
    
    if not draft:
        raise HTTPException(
            status_code=404,
            detail={
                "code": "DRAFT_NOT_FOUND",
                "message": "해당 ID의 특허 초안을 찾을 수 없습니다."
            }
        )
    
    # 유사도 분석 결과 조회
    similarity_query = """
    SELECT * FROM similarity
    WHERE patent_draft_id = :patent_draft_id
    ORDER BY similarity_created_at DESC
    LIMIT 1
    """
    
    similarity = await database.fetch_one(
        query=similarity_query,
        values={"patent_draft_id": patent_draft_id}
    )
    
    if not similarity:
        raise HTTPException(
            status_code=404,
            detail={
                "code": "SIMILARITY_NOT_FOUND",
                "message": "해당 특허 초안의 유사도 분석 결과가 없습니다."
            }
        )
    
    # Record 객체를 딕셔너리로 변환
    draft_dict = dict(draft)
    similarity_dict = dict(similarity)
    
    # 유사 특허 목록 조회
    similar_patents_query = """
    SELECT sp.*, p.patent_title, p.patent_application_number
    FROM similarity_patent sp
    JOIN patent p ON sp.patent_id = p.patent_id
    WHERE sp.similarity_id = :similarity_id
    ORDER BY sp.similarity_patent_score DESC
    """
    
    similar_patents = await database.fetch_all(
        query=similar_patents_query,
        values={"similarity_id": similarity_dict["similarity_id"]}
    )
    
    # 상세 비교 결과 조회
    comparisons_query = """
    SELECT * FROM detailed_comparison
    WHERE patent_draft_id = :patent_draft_id
    ORDER BY detailed_comparison_total_score DESC
    """
    
    comparisons = await database.fetch_all(
        query=comparisons_query,
        values={"patent_draft_id": patent_draft_id}
    )
    
    # 결과 포맷팅
    similar_patents_data = []
    for patent in similar_patents:
        # Record 객체를 딕셔너리로 변환
        patent_dict = dict(patent)
        similar_patents_data.append({
            "patent_id": patent_dict["patent_id"],
            "patent_title": patent_dict["patent_title"],
            "patent_application_number": patent_dict["patent_application_number"],
            "similarity_score": patent_dict["similarity_patent_score"],
            "title_score": patent_dict["similarity_patent_title"],
            "summary_score": patent_dict["similarity_patent_summary"],
            "claim_score": patent_dict["similarity_patent_claim"]
        })
    
    comparisons_data = []
    for comp in comparisons:
        # Record 객체를 딕셔너리로 변환
        comp_dict = dict(comp)
        context_data = comp_dict["detailed_comparison_context"]
        # 컨텍스트 데이터가 JSON 문자열인 경우 파싱
        if context_data and isinstance(context_data, str):
            try:
                context_data = json.loads(context_data)
            except json.JSONDecodeError:
                context_data = {}
        
        comparisons_data.append({
            "patent_id": comp_dict.get("patent_id"),  # patent_id 필드가 없을 수 있으므로 get 사용
            "total_score": comp_dict["detailed_comparison_total_score"],
            "context": context_data
        })
    
    return {
        "data": {
            "similarity_id": similarity_dict["similarity_id"],
            "patent_draft_id": patent_draft_id,
            "similar_patents": similar_patents_data,
            "comparisons": comparisons_data,
            "created_at": similarity_dict["similarity_created_at"]
        }
    }

async def perform_fitness_check(patent_draft_id: int, draft, now: datetime) -> Dict:
    """적합도 검사 수행"""
    print(f"적합도 검사 실행: 특허 초안 ID {patent_draft_id}")
    
    # 레코드 객체를 딕셔너리로 변환
    draft_dict = dict(draft) if draft else {}
    
    # 검사 결과 저장용 딕셔너리
    fitness_results = {
        "is_corrected": True,
        "details": {}
    }

    # 각 섹션별 적합도 검사
    sections = [
        ("technical_field", "기술 분야"),
        ("background", "배경 기술"),
        ("problem", "해결 과제"),
        ("solution", "해결 방법"),
        ("effect", "효과"),
        ("detailed", "구체적인 내용"),  # 추가된 부분
        ("summary", "요약"),
        ("claim", "청구항")
    ]

    for section_key, section_name in sections:
        # 딕셔너리 메소드 사용
        field_key = f"patent_draft_{section_key}"
        section_text = draft_dict.get(field_key, "")
        
        # 1. 내용 존재 확인
        if not section_text or len(section_text.strip()) < 10:
            fitness_results["is_corrected"] = False
            fitness_results["details"][section_key] = False
            continue

        # 2. 개선된 문맥 적합도 검사 사용
        try:
            similarity_score = improved_context_fitness(draft_dict, section_key, section_text)
            # 적합도 기준 점수를 0.4로 낮춤 (더 관대하게)
            if similarity_score < 0.4:
                fitness_results["is_corrected"] = False
                fitness_results["details"][section_key] = False
            else:
                fitness_results["details"][section_key] = True
        except Exception as e:
            print(f"{section_name} 문맥 적합도 검사 중 오류: {str(e)}")
            fitness_results["is_corrected"] = False
            fitness_results["details"][section_key] = False
    
    # 데이터베이스에 적합도 결과 저장
    try:
        fitness_query = """
        INSERT INTO fitness (
            patent_draft_id,
            fitness_is_corrected,
            fitness_good_content,
            fitness_created_at,
            fitness_updated_at
        ) VALUES (
            :patent_draft_id,
            :is_corrected,
            :good_content,
            :created_at,
            :updated_at
        )
        """
        
        await database.execute(
            query=fitness_query,
            values={
                "patent_draft_id": patent_draft_id,
                "is_corrected": fitness_results["is_corrected"],
                "good_content": json.dumps(fitness_results["details"]),
                "created_at": now,
                "updated_at": now
            }
        )
        print(f"적합도 검사 결과 저장 완료: {fitness_results['is_corrected']}")
    except Exception as e:
        print(f"적합도 결과 저장 중 오류: {str(e)}")
    
    return fitness_results

async def perform_detailed_comparison(patent_draft_id: int, draft, top_results: List[Dict], now: datetime):
    """
    상위 특허에 대한 상세 비교 및 KIPRIS API 연동 - KLUE-BERT 모델 활용한 문맥 비교
    상위 1개 특허만 처리
    """
    print(f"상세 비교 시작: 특허 초안 ID {patent_draft_id}, 상위 특허 수: {len(top_results)}")
    
    # 레코드 객체를 딕셔너리로 변환
    draft_dict = dict(draft) if draft else {}
    
    # 상위 1개 특허만 처리
    if len(top_results) > 0:
        patent = top_results[0]  # 상위 1개만 처리
        try:
            print(f"상세 비교 진행 중: 상위 1위 특허")
            patent_app_number = patent["patent_application_number"]
            
            # 1. 특허 공고전문 존재여부 확인, 없으면 KIPRIS API를 통해 가져오기
            patent_public_check = """
            SELECT pp.patent_public_id, pp.patent_public_content 
            FROM patent_public pp 
            WHERE pp.patent_id = :patent_id
            """
            
            public_info = await database.fetch_one(
                query=patent_public_check,
                values={"patent_id": patent["patent_id"]}
            )
            
            # 공고전문 정보가 없으면 KIPRIS API 호출
            if not public_info:
                print(f"KIPRIS API 호출 시작: {patent_app_number}")
                # 새로 정의한 함수 사용
                patent_public_id = await save_patent_public(patent["patent_id"], patent_app_number)
                if not patent_public_id:
                    return  # 실패 시 종료
                    
                # 저장된 공고전문 정보 다시 조회
                public_info = await database.fetch_one(
                    query=patent_public_check,
                    values={"patent_id": patent["patent_id"]}
                )
            else:
                patent_public_id = public_info["patent_public_id"]
            
            # 원본 공고전문 텍스트 (OCR 추출)
            patent_text = public_info["patent_public_content"]
            
            # 2. KLUE-BERT 모델을 활용한 특허 초안과 공고전문의 N:1 문맥 비교
            # 2. 요청대로 solution, detailed, claim 3가지 영역만 처리
            important_sections = {
                "SOLUTION": draft_dict.get("patent_draft_solution", ""),
                "DETAILED": draft_dict.get("patent_draft_detailed", ""),
                "CLAIM": draft_dict["patent_draft_claim"]
            }
            
            # 병렬 처리 코드
            section_tasks = []
            for section_key, section_text in important_sections.items():
                if section_text and len(section_text.strip()) >= 10:
                    task = process_section(section_key, section_text, patent_text)
                    section_tasks.append(task)

            # 모든 섹션 병렬 처리 실행
            results = await asyncio.gather(*section_tasks)
            highlights = [r for r in results if r is not None]
            
            # 전체 유사도 계산
            overall_similarity = 0.0
            if highlights:
                overall_similarity = sum(h["similarity_score"] for h in highlights) / len(highlights)
            
            # 3. 상세 비교 결과 저장
            context = {
                "highlights": highlights
            }
            
            detailed_comparison_query = """
            INSERT INTO detailed_comparison (
                patent_draft_id,
                similarity_patent_id,
                patent_public_id,
                detailed_comparison_total_score,
                detailed_comparison_context,
                detailed_comparison_result,
                detailed_comparison_created_at,
                detailed_comparison_updated_at
            ) VALUES (
                :draft_id,
                :similarity_patent_id,
                :public_id,
                :total_score,
                :context,
                :result,
                :created_at,
                :updated_at
            )
            """

            print(f"상세 비교 결과 저장 시작: {patent_app_number}")
            await database.execute(
                query=detailed_comparison_query,
                values={
                    "draft_id": patent_draft_id,
                    "similarity_patent_id": patent.get("similarity_patent_id", None),
                    "public_id": patent_public_id,
                    "total_score": overall_similarity,
                    "context": json.dumps(context, ensure_ascii=False),
                    "result": json.dumps({"similar": overall_similarity >= 0.7}, ensure_ascii=False),
                    "created_at": now,
                    "updated_at": now
                }
            )
            
            print(f"특허 {patent_app_number}의 상세 비교 완료")
            
        except Exception as e:
            print(f"특허 {patent.get('patent_application_number', '알 수 없음')} 상세 비교 중 오류: {str(e)}")
            import traceback
            print(traceback.format_exc())
    
    print(f"상세 비교 완료: 특허 초안 ID {patent_draft_id}")
    return

# 개선된 문맥 적합도 검사 함수
def improved_context_fitness(draft_dict, section_key, section_text):
    """개선된 문맥 적합도 검사 - 다중 패턴 및 주제 관련성 평가"""
    try:
        # 1. 섹션별 다중 패턴 예시 정의 - 더 많은 패턴 추가
        field_examples = {
            "technical_field": [
                "본 발명은 기술 분야에 관한 것으로", 
                "본 기술은 다음 분야와 관련된다", 
                "이 발명은 다음 영역에 속한다",
                "본 발명은 다음 분야에 관한 것이다",
                "본 발명의 기술 분야는",
                "본 기술은 다음과 관련이 있다"
            ],
            "background": [
                "종래 기술에서는 다음과 같은 문제점이 있었다", 
                "기존 시스템은 다음과 같은 한계가 있다", 
                "현재 사용되는 방식은 여러 단점을 가지고 있다",
                "종래에는 다음과 같은 방식으로 구현되었다",
                "기존의 기술에서는",
                "종래 기술의 문제점으로는"
            ],
            "problem": [
                "본 발명이 해결하고자 하는 과제는", 
                "본 기술의 목적은", 
                "해결해야 할 문제는",
                "본 발명의 목적은",
                "본 발명의 주요 과제는",
                "이 발명이 해결하려는 문제는"
            ],
            "solution": [
                "상기 과제를 해결하기 위한 본 발명의 구성은", 
                "이 문제를 해결하기 위해", 
                "본 발명에서 제안하는 해결책은",
                "본 발명의 주요 구성은",
                "위 목적을 달성하기 위한 본 발명은",
                "상기 과제 해결을 위해 본 발명은"
            ],
            "effect": [
                "본 발명에 따르면 다음과 같은 효과가 있다", 
                "본 기술의 장점은", 
                "이 발명의 효과로는",
                "본 발명의 주요 효과는",
                "본 발명에 의하면",
                "본 발명의 장점으로는"
            ],
            "detailed": [
                "본 시스템은 다음과 같은 구성요소를 포함한다", # 현재 초안 패턴
                "본 발명의 실시예는 다음과 같다", 
                "구체적인 구현 방법으로는",
                "본 발명의 바람직한 실시 형태는",
                "본 발명의 구체적인 구성은",
                "본 발명의 상세한 설명은 다음과 같다",
                "각 모듈은 다음과 같이 구성된다"
            ],
            "summary": [
                "본 발명을 요약하면", 
                "요약하자면", 
                "본 기술은 다음과 같이 요약된다",
                "본 발명의 핵심은",
                "간략히 정리하면",
                "본 발명을 간단히 설명하면"
            ],
            "claim": [
                "청구항 1.", 
                "다음을 포함하는", 
                "특징으로 하는",
                "청구항",
                "다음으로 구성된",
                "포함하는 것을 특징으로 하는"
            ]
        }
        
        # 2. 섹션별 키워드 정의 - 더 많은 키워드 추가
        section_keywords = {
            "technical_field": ["분야", "기술", "관한", "관련된", "특히", "영역", "응용", "적용"],
            "background": ["기존", "종래", "문제", "한계", "단점", "현재", "지금까지", "종전", "기술적 배경"],
            "problem": ["과제", "해결", "목적", "문제", "필요성", "요구", "이슈", "개선", "도전"],
            "solution": ["구성", "수단", "방법", "구현", "포함", "해결책", "설계", "방안", "접근법"],
            "effect": ["효과", "장점", "개선", "향상", "감소", "절감", "증가", "이점", "우위"],
            "detailed": ["실시예", "구현", "구체적", "상세", "도면", "실시", "방법", "구성요소", "포함", "시스템", "컴포넌트", "모듈", "동작", "설명", "방식"],
            "summary": ["요약", "정리", "줄이면", "기술", "종합", "간략", "핵심", "간단히", "포괄적"],
            "claim": ["청구항", "포함", "구성", "특징", "방법", "장치", "시스템", "구성된", "이루어진"]
        }
        
        # 3. 섹션 텍스트의 BERT 벡터 생성
        section_vector = get_bert_vector(section_text)
        
        # 4. 다중 패턴 접근법 - 가장 높은 유사도 점수 사용
        pattern_scores = []
        for example in field_examples.get(section_key, ["특허 문서 텍스트"]):
            example_vector = get_bert_vector(example)
            similarity = safe_cosine_similarity(section_vector, example_vector)
            pattern_scores.append(similarity)
        
        pattern_score = max(pattern_scores) if pattern_scores else 0.0
        
        # 5. 주제와의 관련성 평가
        title_text = draft_dict.get("patent_draft_title", "")
        title_vector = get_bert_vector(title_text)
        relevance_score = safe_cosine_similarity(title_vector, section_vector)
        
        # 6. 키워드 포함 여부 확인
        keywords = section_keywords.get(section_key, [])
        keyword_matches = sum(1 for keyword in keywords if keyword in section_text)
        keyword_score = min(1.0, keyword_matches / max(1, len(keywords)))
        
        # 7. 가중 평균으로 최종 점수 계산
        final_score = (0.4 * pattern_score) + (0.4 * relevance_score) + (0.2 * keyword_score)
        
        # 디버깅용
        print(f"섹션: {section_key}, 패턴 점수: {pattern_score:.2f}, 관련성 점수: {relevance_score:.2f}, 키워드 점수: {keyword_score:.2f}, 최종: {final_score:.2f}")
        
        return final_score
    except Exception as e:
        print(f"개선된 문맥 적합도 검사 중 오류: {str(e)}")
        return 0.0  # 오류 시 0점 반환

@router.post("/patent/{patent_draft_id}/similarity", response_model=Dict[str, Any])
async def create_similarity(patent_draft_id: int):
    """특허 초안의 유사도 검사 실행 - KNN 기반 검색 사용"""
    try:
        # 특허 초안 존재 확인
        draft_query = """
        SELECT * FROM patent_draft 
        WHERE patent_draft_id = :draft_id
        """
        draft = await database.fetch_one(
            query=draft_query,
            values={"draft_id": patent_draft_id}
        )
        
        if not draft:
            return {
                "status": False,
                "code": 404,
                "data": None,
                "error": {
                    "code": "PATENT_DRAFT_NOT_FOUND",
                    "message": "해당 ID의 특허 초안을 찾을 수 없습니다."
                }
            }
        
        # KNN 기반 유사도 검색 수행
        from app.services.knn_search import perform_knn_search
        result = await perform_knn_search(patent_draft_id, k=20)
        
        if not result:
            return {
                "status": False,
                "code": 500,
                "data": None,
                "error": {
                    "code": "SEARCH_FAILED",
                    "message": "유사도 검색 실패"
                }
            }
        
        # 적합도 검사 수행
        now = datetime.now(timezone.utc)
        fitness_results = await perform_fitness_check(patent_draft_id, draft, now)
        
        # 상세 비교 (상위 1개만)
        if result["results"]:
            await perform_detailed_comparison(patent_draft_id, draft, result["results"][:1], now)
        
        return {
            "data": {
                "similarity_id": result["similarity_id"],
                "execution_time_seconds": result.get("execution_time_seconds"),
                "search_method": "knn"
            }
        }
        
    except Exception as e:
        logger.error(f"유사도 검사 중 오류 발생: {str(e)}")
        logger.error(traceback.format_exc())
        
        return {
            "status": False,
            "code": 500,
            "data": None,
            "error": {
                "code": "SIMILARITY_ERROR",
                "message": f"유사도 검사 중 오류 발생: {str(e)}"
            }
        }

async def save_patent_public(patent_id: int, application_number: str) -> int:
    """KIPRIS API를 호출하여 특허 공고전문을 가져와 저장합니다."""
    try:
        from app.api.routes.patent_public import get_patent_public
        
        # API 호출하여 공고전문 가져오기
        response = await get_patent_public(application_number)
        
        if "data" in response and response.get("data"):
            return response["data"].get("patent_public_id")
        else:
            logger.error(f"공고전문 저장 실패: {application_number}")
            return None
    except Exception as e:
        logger.error(f"공고전문 저장 중 오류: {str(e)}")
        return None
