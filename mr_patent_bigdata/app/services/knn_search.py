import numpy as np
import logging
from typing import List, Dict, Any
from datetime import datetime, timezone
import heapq
from app.core.database import database
from app.services.vectorizer import get_tfidf_vector
from app.api.routes.similarity import safe_frombuffer, safe_cosine_similarity

logger = logging.getLogger(__name__)

# 벡터 캐싱용 전역 변수 (메모리에 미리 로드)
cached_vectors = None

# async def load_vectors_to_memory(limit=None):
#     """데이터베이스에서 특허 벡터를 메모리에 로드 (IPC 'H' 필터링)"""
#     global cached_vectors
    
#     if cached_vectors is not None:
#         return cached_vectors
    
#     logger.info("특허 벡터 메모리 로드 시작")

#     query = """
#     SELECT p.patent_id, p.patent_title, p.patent_application_number,
#            p.patent_title_tfidf_vector, p.patent_summary_tfidf_vector, p.patent_claim_tfidf_vector
#     FROM patent p
#     """
    
#     if limit:
#         query += f" LIMIT {limit}"
    
#     patents = await database.fetch_all(query=query)
    
#     vectors = []
#     for patent in patents:
#         try:
#             # 벡터 로드 및 결합
#             title_vector = safe_frombuffer(patent["patent_title_tfidf_vector"], target_dim=1000)
#             summary_vector = safe_frombuffer(patent["patent_summary_tfidf_vector"], target_dim=1000)
#             claim_vector = safe_frombuffer(patent["patent_claim_tfidf_vector"], target_dim=1000)
            
#             # 유효성 검사 (영벡터 제외)
#             if not np.all(title_vector == 0) or not np.all(summary_vector == 0) or not np.all(claim_vector == 0):
#                 # 가중치 적용한 결합 벡터
#                 combined_vector = title_vector * 0.3 + summary_vector * 0.3 + claim_vector * 0.4
                
#                 vectors.append({
#                     "patent_id": patent["patent_id"],
#                     "title": patent["patent_title"],
#                     "application_number": patent["patent_application_number"],
#                     "vector": combined_vector
#                 })

#             logger.debug(f"벡터 형태: title={title_vector.shape}, sum={np.sum(title_vector)}")
            
#         except Exception as e:
#             logger.error(f"특허 ID {patent['patent_id']} 벡터 처리 중 오류: {str(e)}")
    
#     cached_vectors = vectors
#     logger.info(f"특허 벡터 {len(vectors)}개 메모리 로드 완료")
#     return vectors

async def load_vectors_to_memory(limit=None):
    """데이터베이스에서 특허 벡터를 메모리에 로드"""
    global cached_vectors
    
    if cached_vectors is not None:
        return cached_vectors
    
    logger.info("특허 벡터 메모리 로드 시작")

    query = """
    SELECT p.patent_id, p.patent_title, p.patent_application_number,
           p.patent_title_tfidf_vector, p.patent_summary_tfidf_vector, p.patent_claim_tfidf_vector
    FROM patent p
    """
    
    if limit:
        query += f" LIMIT {limit}"
    
    patents = await database.fetch_all(query=query)
    
    vectors = []
    for patent in patents:
        try:
            # 벡터 로드 및 결합
            title_vector = safe_frombuffer(patent["patent_title_tfidf_vector"], target_dim=1000)
            summary_vector = safe_frombuffer(patent["patent_summary_tfidf_vector"], target_dim=1000)
            claim_vector = safe_frombuffer(patent["patent_claim_tfidf_vector"], target_dim=1000)
            
            # 유효성 검사 제거 - 모든 벡터 포함
            # 가중치 적용한 결합 벡터
            combined_vector = title_vector * 0.3 + summary_vector * 0.3 + claim_vector * 0.4
            
            vectors.append({
                "patent_id": patent["patent_id"],
                "title": patent["patent_title"],
                "application_number": patent["patent_application_number"],
                "vector": combined_vector
            })
            
            # 디버깅 로그 추가
            if len(vectors) % 10000 == 0:
                logger.info(f"벡터 {len(vectors)}개 로드됨...")
            
        except Exception as e:
            logger.error(f"특허 ID {patent['patent_id']} 벡터 처리 중 오류: {str(e)}")
    
    cached_vectors = vectors
    logger.info(f"특허 벡터 {len(vectors)}개 메모리 로드 완료")
    return vectors

async def perform_knn_search(patent_draft_id: int, k: int = 20):
    """K-Nearest Neighbors 기반 유사도 검색"""
    start_time = datetime.now()
    logger.info(f"KNN 유사도 검색 시작: 특허 초안 ID {patent_draft_id}")
    
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
    except Exception as e:
        logger.error(f"특허 초안 벡터 처리 중 오류: {str(e)}")
        return None
    
    # 3. 메모리에 특허 벡터 로드
    patent_vectors = await load_vectors_to_memory()
    
    # 4. KNN 검색 - 힙 자료구조를 활용한 효율적인 구현
    top_k = []  # (유사도, 특허 정보) 형태의 최소 힙
    
    for patent in patent_vectors:
        try:
            similarity = safe_cosine_similarity(query_vector, patent["vector"])
            
            # 상위 k개 유지 (최소 힙)
            if len(top_k) < k:
                heapq.heappush(top_k, (similarity, patent))
            elif similarity > top_k[0][0]:
                heapq.heappushpop(top_k, (similarity, patent))
        except Exception as e:
            logger.error(f"유사도 계산 중 오류: {str(e)}")
    
    # 5. 결과 정렬 (내림차순)
    results = [(sim, pat) for sim, pat in top_k]
    results.sort(reverse=True)
    
    # 6. 유사도 결과 저장
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
    for i, (similarity, patent) in enumerate(results):
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
    logger.info(f"KNN 유사도 검색 완료: {len(formatted_results)}개 결과 ({duration:.2f}초 소요)")
    
    return {
        "similarity_id": similarity_id,
        "results": formatted_results,
        "execution_time_seconds": duration
    }
