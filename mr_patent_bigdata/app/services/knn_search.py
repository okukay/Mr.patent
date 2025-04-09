import numpy as np
import logging
from typing import List, Dict, Any, Tuple, Optional
from datetime import datetime, timezone
import faiss  # FAISS 라이브러리 추가
from app.core.database import database
from app.services.vectorizer import get_tfidf_vector
from app.api.routes.similarity import safe_frombuffer, safe_cosine_similarity

logger = logging.getLogger(__name__)

# FAISS 인덱스를 저장할 전역 변수
_FAISS_INDEX = None
_INDEX_TO_PATENT_ID = []
_PATENT_DETAILS = {}

async def build_faiss_index(force_rebuild=False) -> Tuple[faiss.Index, List[int], Dict[int, Dict]]:
    """특허 벡터를 사용하여 FAISS 인덱스를 생성합니다."""
    global _FAISS_INDEX, _INDEX_TO_PATENT_ID, _PATENT_DETAILS
    
    # 이미 인덱스가 있고 강제 재구축이 아니면 캐시된 인덱스 반환
    if _FAISS_INDEX is not None and not force_rebuild:
        logger.info(f"캐시된 FAISS 인덱스 사용 (벡터 수: {len(_INDEX_TO_PATENT_ID)})")
        return _FAISS_INDEX, _INDEX_TO_PATENT_ID, _PATENT_DETAILS
    
    start_time = datetime.now()
    logger.info("FAISS 인덱스 구축 시작")
    
    # 데이터베이스에서 모든 특허 벡터 로딩
    batch_size = 5000
    offset = 0
    patent_vectors = []
    patent_ids = []
    patent_details = {}
    
    while True:
        # 배치 단위로 특허 데이터 로드
        query = f"""
        SELECT p.patent_id, p.patent_title, p.patent_application_number,
               p.patent_title_tfidf_vector, p.patent_summary_tfidf_vector, p.patent_claim_tfidf_vector
        FROM patent p
        ORDER BY p.patent_id
        LIMIT {offset}, {batch_size}
        """
        
        batch_data = await database.fetch_all(query=query)
        
        # 더 이상 데이터가 없으면 종료
        if not batch_data or len(batch_data) == 0:
            break
            
        logger.info(f"FAISS 인덱스 구축: 배치 {offset//batch_size + 1} - {len(batch_data)}개 특허 처리 중...")
        
        # 각 특허 벡터 처리
        for patent in batch_data:
            try:
                # 특허 벡터 로드
                title_vector = safe_frombuffer(patent["patent_title_tfidf_vector"], target_dim=1000)
                summary_vector = safe_frombuffer(patent["patent_summary_tfidf_vector"], target_dim=1000)
                claim_vector = safe_frombuffer(patent["patent_claim_tfidf_vector"], target_dim=1000)
                
                # 결합 벡터 생성 (가중합)
                combined_vector = title_vector * 0.3 + summary_vector * 0.3 + claim_vector * 0.4
                
                patent_id = patent["patent_id"]
                patent_vectors.append(combined_vector)
                patent_ids.append(patent_id)
                
                # 특허 정보 저장
                patent_details[patent_id] = {
                    "patent_id": patent_id,
                    "title": patent["patent_title"],
                    "application_number": patent["patent_application_number"],
                }
                
            except Exception as e:
                logger.error(f"특허 ID {patent['patent_id']} 벡터 처리 중 오류: {str(e)}")
        
        # 다음 배치로 이동
        offset += len(batch_data)
    
    # 벡터 데이터가 없으면 종료
    if not patent_vectors:
        logger.error("벡터 데이터가 없어 FAISS 인덱스를 구축할 수 없습니다.")
        return None, [], {}
    
    # NumPy 배열로 변환
    vector_array = np.array(patent_vectors, dtype=np.float32)
    
    # FAISS 인덱스 구축
    vector_dimension = vector_array.shape[1]
    index = faiss.IndexFlatIP(vector_dimension)  # 내적(코사인 유사도)을 사용하는 플랫 인덱스
    
    # 정규화 (L2 norm = 1)하여 내적이 코사인 유사도가 되게 함
    faiss.normalize_L2(vector_array)
    
    # 인덱스에 벡터 추가
    index.add(vector_array)
    
    # 메모리에 결과 저장
    _FAISS_INDEX = index
    _INDEX_TO_PATENT_ID = patent_ids
    _PATENT_DETAILS = patent_details
    
    end_time = datetime.now()
    duration = (end_time - start_time).total_seconds()
    logger.info(f"FAISS 인덱스 구축 완료: {len(patent_vectors)}개 특허, {duration:.2f}초 소요")
    
    return index, patent_ids, patent_details

async def perform_knn_search(patent_draft_id: int, k: int = 20):
    """FAISS 기반 K-Nearest Neighbors 유사도 검색"""
    start_time = datetime.now()
    logger.info(f"FAISS 기반 KNN 유사도 검색 시작: 특허 초안 ID {patent_draft_id}")
    
    # 1. 특허 초안 데이터 가져오기
    query = """
    SELECT patent_draft_id, patent_draft_title, patent_draft_summary, patent_draft_claim,
           patent_draft_title_tfidf_vector, patent_draft_summary_tfidf_vector, patent_draft_claim_tfidf_vector
    FROM patent_draft
    WHERE patent_draft_id = :patent_draft_id
    """
    
    draft = await database.fetch_one(
        query=query,
        values={"patent_draft_id": patent_draft_id}
    )
    
    if not draft:
        logger.error(f"특허 초안을 찾을 수 없음: {patent_draft_id}")
        return None
    
    # 2. 특허 초안 벡터 가져오기 (저장된 벡터 사용)
    try:
        title_vector = safe_frombuffer(draft["patent_draft_title_tfidf_vector"], target_dim=1000)
        summary_vector = safe_frombuffer(draft["patent_draft_summary_tfidf_vector"], target_dim=1000)
        claim_vector = safe_frombuffer(draft["patent_draft_claim_tfidf_vector"], target_dim=1000)
        
        # 벡터가 없으면 텍스트에서 생성 (fallback)
        if np.all(title_vector == 0) and draft["patent_draft_title"]:
            title_vector = get_tfidf_vector(draft["patent_draft_title"])
        if np.all(summary_vector == 0) and draft["patent_draft_summary"]:
            summary_vector = get_tfidf_vector(draft["patent_draft_summary"])
        if np.all(claim_vector == 0) and draft["patent_draft_claim"]:
            claim_vector = get_tfidf_vector(draft["patent_draft_claim"])
        
        # 결합 벡터 생성
        query_vector = title_vector * 0.3 + summary_vector * 0.3 + claim_vector * 0.4
        
        # 벡터 정규화 (FAISS 인덱스와 일치)
        query_vector = query_vector.astype(np.float32)
        query_vector = query_vector.reshape(1, -1)  # 2D 배열로 변환
        faiss.normalize_L2(query_vector)
        
    except Exception as e:
        logger.error(f"특허 초안 벡터 처리 중 오류: {str(e)}")
        return None
    
    # 3. FAISS 인덱스 로드 또는 생성
    index, patent_ids, patent_details = await build_faiss_index()
    
    if index is None:
        logger.error("FAISS 인덱스를 불러올 수 없습니다.")
        return None
    
    # 4. FAISS를 사용한 KNN 검색
    try:
        # FAISS 검색 실행 (k+5: 혹시 모를 필터링에 대비)
        search_k = min(k + 5, len(patent_ids))
        distances, indices = index.search(query_vector, search_k)
        
        # 결과 처리 (유사도가 높은 순으로 정렬)
        results = []
        
        for i, (idx, distance) in enumerate(zip(indices[0], distances[0])):
            if idx >= len(patent_ids):  # 인덱스 범위 확인
                continue
                
            # 내적은 1에 가까울수록 유사도가 높음
            similarity = float(distance)
            patent_id = patent_ids[idx]
            
            if patent_id in patent_details:
                result = {
                    "patent_id": patent_id,
                    "title": patent_details[patent_id]["title"],
                    "application_number": patent_details[patent_id]["application_number"],
                    "overall_similarity": similarity
                }
                results.append((similarity, result))
        
        # 상위 k개만 유지 (이미 정렬되어 있음)
        top_results = results[:k]
        
    except Exception as e:
        logger.error(f"FAISS 검색 중 오류: {str(e)}")
        import traceback
        logger.error(traceback.format_exc())
        return None
    
    # 5. 유사도 결과 저장
    now = datetime.now(timezone.utc)
    
    # similarity 테이블에 레코드 생성
    similarity_query = """
    INSERT INTO similarity (
        patent_draft_id, similarity_created_at, similarity_updated_at
    ) VALUES (
        :patent_draft_id, :created_at, :updated_at
    )
    """
    
    similarity_id = await database.execute(
        query=similarity_query,
        values={
            "patent_draft_id": patent_draft_id,
            "created_at": now,
            "updated_at": now
        }
    )
    
    # 각 유사 특허 저장
    formatted_results = []
    for i, (similarity, patent) in enumerate(top_results):
        similarity_patent_query = """
        INSERT INTO similarity_patent (
            similarity_id, patent_id, similarity_patent_score,
            similarity_patent_title, similarity_patent_claim, similarity_patent_summary,
            similarity_patent_created_at, similarity_patent_updated_at
        ) VALUES (
            :similarity_id, :patent_id, :score,
            :title_score, :claim_score, :summary_score,
            :created_at, :updated_at
        )
        """
        
        similarity_patent_id = await database.execute(
            query=similarity_patent_query,
            values={
                "similarity_id": similarity_id,
                "patent_id": patent["patent_id"],
                "score": similarity,
                "title_score": similarity,  # 현재는 동일한 점수 사용
                "claim_score": similarity,
                "summary_score": similarity,
                "created_at": now,
                "updated_at": now
            }
        )
        
        formatted_results.append({
            "patent_id": patent["patent_id"],
            "patent_title": patent["title"],
            "patent_application_number": patent.get("application_number", ""),
            "similarity_patent_id": similarity_patent_id,
            "overall_similarity": similarity
        })
    
    end_time = datetime.now()
    duration = (end_time - start_time).total_seconds()
    logger.info(f"FAISS 기반 KNN 유사도 검색 완료: {len(formatted_results)}개 결과 ({duration:.2f}초 소요)")
    
    return {
        "similarity_id": similarity_id,
        "results": formatted_results,
        "execution_time_seconds": duration
    }

# 호환성을 위한 더미 함수
async def load_vectors_to_memory(limit=None, batch_size=1000):
    """호환성을 위한 더미 함수 - FAISS 기반 방식으로 전환되어 벡터를 미리 로드하지 않음"""
    logger.info("벡터 로드 함수가 호출되었지만, FAISS 기반 방식으로 전환되어 벡터를 미리 로드하지 않습니다")
    return []
