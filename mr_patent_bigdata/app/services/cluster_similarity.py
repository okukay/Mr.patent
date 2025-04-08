import numpy as np
import json
import logging
import datetime
import pickle
import os
from app.core.database import database

logger = logging.getLogger(__name__)

async def perform_cluster_based_similarity_search(patent_draft_id, top_k=20):
    """클러스터 기반 유사도 검색 구현"""
    start_time = datetime.datetime.now()
    logger.info(f"클러스터 기반 유사도 검색 시작: 특허 초안 ID {patent_draft_id}")
    
    # 1. 특허 초안 데이터 가져오기
    query = """
    SELECT patent_draft_id, patent_draft_title, patent_draft_summary, patent_draft_claim
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
    
    # 2. 특허 초안 텍스트 벡터화 (기존 함수 사용)
    from app.services.vectorizer import get_tfidf_vector
    
    title_vector = get_tfidf_vector(draft["patent_draft_title"])
    summary_vector = get_tfidf_vector(draft["patent_draft_summary"])
    claim_vector = get_tfidf_vector(draft["patent_draft_claim"])
    
    # 3. 가중치 적용한 결합 벡터 생성
    combined_vector = np.array(title_vector) * 0.3 + np.array(summary_vector) * 0.3 + np.array(claim_vector) * 0.4
    
    # 4. 저장된 PCA 모델 로드 (차원 축소)
    pca_path = "models/patent_pca_model.pkl"
    if os.path.exists(pca_path):
        with open(pca_path, "rb") as f:
            pca = pickle.load(f)
        reduced_vector = pca.transform([combined_vector])[0]
    else:
        logger.warning("PCA 모델을 찾을 수 없음 - 전체 벡터 사용")
        reduced_vector = combined_vector[:100]  # 임시 대안
    
    # 5. 클러스터 중심점 로드 - 테이블 이름 수정: patent_cluster -> cluster
    cluster_query = "SELECT cluster_id, cluster_center_vector FROM cluster"
    clusters = await database.fetch_all(cluster_query)
    
    if not clusters:
        logger.error("클러스터 정보가 없습니다. 군집화를 먼저 실행하세요.")
        return None
    
    # 6. 가장 가까운 클러스터 식별
    closest_clusters = []
    for cluster in clusters:
        center_vector = np.array(json.loads(cluster["cluster_center_vector"]))
        
        # 코사인 유사도 계산 (내적 / (노름 * 노름))
        norm_product = np.linalg.norm(reduced_vector) * np.linalg.norm(center_vector)
        if norm_product == 0:
            similarity = 0
        else:
            similarity = np.dot(reduced_vector, center_vector) / norm_product
        
        closest_clusters.append({
            "cluster_id": cluster["cluster_id"],
            "similarity": similarity
        })
    
    # 유사도 기준 상위 3개 클러스터 선택
    closest_clusters.sort(key=lambda x: x["similarity"], reverse=True)
    selected_clusters = closest_clusters[:3]
    
    logger.info(f"가장 가까운 클러스터: {[c['cluster_id'] for c in selected_clusters]}")
    
    # 7. 선택된 클러스터 내에서 유사도 검색
    results = []
    
    for cluster_info in selected_clusters:
        cluster_id = cluster_info["cluster_id"]
        
        # 클러스터 내 특허 검색
        patent_query = """
        SELECT p.patent_id, p.patent_title, p.patent_summary, p.patent_claim,
               p.patent_title_tfidf_vector, p.patent_summary_tfidf_vector, p.patent_claim_tfidf_vector
        FROM patent p
        WHERE p.cluster_id = :cluster_id
        LIMIT 500  # 클러스터 내 검색 제한
        """
        
        patents = await database.fetch_all(
            query=patent_query,
            values={"cluster_id": cluster_id}
        )
        
        logger.info(f"클러스터 {cluster_id} 내 특허 {len(patents)}개 로드")
        
        # 각 특허와 유사도 계산
        for patent in patents:
            try:
                p_title_vector = np.array(json.loads(patent["patent_title_tfidf_vector"]))
                p_summary_vector = np.array(json.loads(patent["patent_summary_tfidf_vector"]))
                p_claim_vector = np.array(json.loads(patent["patent_claim_tfidf_vector"]))
                
                # 결합 벡터
                p_combined_vector = p_title_vector * 0.3 + p_summary_vector * 0.3 + p_claim_vector * 0.4
                
                # PCA 변환 적용
                if 'pca' in locals():
                    p_reduced_vector = pca.transform([p_combined_vector])[0]
                else:
                    p_reduced_vector = p_combined_vector[:100]
                
                # 코사인 유사도 계산
                from app.api.routes.similarity import safe_cosine_similarity
                similarity = safe_cosine_similarity(reduced_vector, p_reduced_vector)
                
                results.append({
                    "patent_id": patent["patent_id"],
                    "title": patent["patent_title"],
                    "similarity": float(similarity),
                    "cluster_id": cluster_id
                })
            except Exception as e:
                logger.error(f"특허 ID {patent['patent_id']} 유사도 계산 중 오류: {str(e)}")
    
    # 8. 결과 정렬 및 상위 N개 선택
    results.sort(key=lambda x: x["similarity"], reverse=True)
    top_results = results[:top_k]
    
    # 9. 유사도 결과 저장
    now = datetime.datetime.now()
    
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
    for i, result in enumerate(top_results):
        similarity_patent_query = """
        INSERT INTO similarity_patent (
            similarity_id, patent_id, similarity_patent_score,
            similarity_patent_created_at, similarity_patent_updated_at,
            similarity_patent_rank
        ) VALUES (
            :similarity_id, :patent_id, :score,
            :created_at, :updated_at, :rank
        )
        """
        
        await database.execute(
            query=similarity_patent_query,
            values={
                "similarity_id": similarity_id,
                "patent_id": result["patent_id"],
                "score": result["similarity"],
                "created_at": now,
                "updated_at": now,
                "rank": i + 1
            }
        )
    
    end_time = datetime.datetime.now()
    duration = (end_time - start_time).total_seconds()
    logger.info(f"클러스터 기반 유사도 검색 완료: {len(top_results)}개 결과 ({duration:.2f}초 소요)")
    
    return {
        "similarity_id": similarity_id,
        "results": top_results,
        "execution_time_seconds": duration
    }
