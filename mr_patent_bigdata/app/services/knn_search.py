import numpy as np
import logging
import os
from typing import List, Dict, Any, Tuple, Optional
from datetime import datetime, timezone
import faiss  # FAISS 라이브러리 추가
from app.core.database import database
from app.services.vectorizer import get_tfidf_vector, get_bert_vector
from app.api.routes.similarity import safe_frombuffer, safe_cosine_similarity
from app.services.vectorizer import safe_vector

logger = logging.getLogger(__name__)

# FAISS 인덱스 저장 경로 설정 (OS 독립적)
current_dir = os.path.dirname(os.path.abspath(__file__))
faiss_dir = os.path.join(current_dir, "..", "..", "faiss_indexes")
os.makedirs(faiss_dir, exist_ok=True)
index_path = os.path.join(faiss_dir, "patent_index.index")
metadata_path = os.path.join(faiss_dir, "patent_index_metadata.npz")

# FAISS 인덱스를 저장할 전역 변수
_FAISS_INDEX = None
_INDEX_TO_PATENT_ID = []
_PATENT_DETAILS = {}

async def build_faiss_index(force_rebuild=False) -> Tuple[faiss.Index, List[int], Dict[int, Dict]]:
    """특허 벡터를 사용하여 FAISS 인덱스를 생성합니다."""
    global _FAISS_INDEX, _INDEX_TO_PATENT_ID, _PATENT_DETAILS
    
    # 이미 메모리에 인덱스가 있고 강제 재구축이 아니면 캐시된 인덱스 반환
    if _FAISS_INDEX is not None and not force_rebuild:
        logger.info(f"메모리에 캐시된 FAISS 인덱스 사용 (벡터 수: {len(_INDEX_TO_PATENT_ID)})")
        return _FAISS_INDEX, _INDEX_TO_PATENT_ID, _PATENT_DETAILS
    
    # 파일에서 인덱스 로드 시도 (강제 재구축이 아닌 경우)
    if os.path.exists(index_path) and os.path.exists(metadata_path) and not force_rebuild:
        try:
            logger.info(f"파일에서 FAISS 인덱스 로드 중: {index_path}")
            _FAISS_INDEX = faiss.read_index(index_path)
            
            metadata = np.load(metadata_path, allow_pickle=True)
            _INDEX_TO_PATENT_ID = metadata['patent_ids'].tolist()
            _PATENT_DETAILS = metadata['patent_details'].item()
            
            logger.info(f"FAISS 인덱스 로드 완료: {len(_INDEX_TO_PATENT_ID)}개 특허")
            return _FAISS_INDEX, _INDEX_TO_PATENT_ID, _PATENT_DETAILS
        except Exception as e:
            logger.error(f"파일에서 FAISS 인덱스 로드 실패: {str(e)}")
            # 로드 실패 시 새로 구축 진행
    
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
    
    # 벡터가 모두 0인지 확인
    zero_vectors = np.all(vector_array == 0, axis=1)
    if np.any(zero_vectors):
        logger.warning(f"0인 벡터가 {np.sum(zero_vectors)}개 발견됨. 이 벡터들은 검색 결과가 정확하지 않을 수 있습니다.")

    # 벡터 노름 확인
    norms = np.linalg.norm(vector_array, axis=1)
    logger.info(f"벡터 노름 평균: {np.mean(norms):.6f}, 최소: {np.min(norms):.6f}, 최대: {np.max(norms):.6f}")

    # FAISS 인덱스 구축
    vector_dimension = vector_array.shape[1]
    index = faiss.IndexFlatIP(vector_dimension)  # 내적(코사인 유사도)을 사용하는 플랫 인덱스

    # 정규화 (L2 norm = 1)하여 내적이 코사인 유사도가 되게 함
    faiss.normalize_L2(vector_array)

    # 정규화 후 노름 다시 확인 (디버깅용)
    norms_after = np.linalg.norm(vector_array, axis=1)
    logger.info(f"정규화 후 벡터 노름 평균: {np.mean(norms_after):.6f}")

    # 인덱스에 벡터 추가
    index.add(vector_array)

    # 메모리에 결과 저장
    _FAISS_INDEX = index
    _INDEX_TO_PATENT_ID = patent_ids
    _PATENT_DETAILS = patent_details
    
    # 인덱스를 파일에 저장 (OS 독립적 경로)
    try:
        logger.info(f"FAISS 인덱스 파일 저장 중: {index_path}")
        faiss.write_index(index, index_path)
        
        # 메타데이터 저장 (patent_ids와 patent_details)
        np.savez(metadata_path, 
                 patent_ids=np.array(patent_ids),
                 patent_details=patent_details)
        
        logger.info(f"FAISS 인덱스 파일 저장 완료")
    except Exception as e:
        logger.error(f"FAISS 인덱스 파일 저장 실패: {str(e)}")
    
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
    
    # 2. 특허 초안 벡터 가져오기 (BERT 벡터 사용으로 수정)
    try:
        # 벡터라이저 어휘 확장을 위해 텍스트 결합
        combined_text = f"{draft['patent_draft_title']} {draft['patent_draft_summary']} {draft['patent_draft_claim']}"
        
        # BERT 벡터 사용으로 변경
        from app.services.vectorizer import get_bert_vector
        
        # 각 필드의 BERT 벡터 계산
        title_vector = get_bert_vector(draft["patent_draft_title"] or "")
        summary_vector = get_bert_vector(draft["patent_draft_summary"] or "")
        claim_vector = get_bert_vector(draft["patent_draft_claim"] or "")
        
        # BERT 벡터는 768차원이지만 FAISS 인덱스는 1000차원으로 구축되어 있음
        # 간단한 해결책: 768차원을 1000차원으로 패딩
        title_padded = np.pad(title_vector, (0, 1000-len(title_vector)), 'constant')
        summary_padded = np.pad(summary_vector, (0, 1000-len(summary_vector)), 'constant')
        claim_padded = np.pad(claim_vector, (0, 1000-len(claim_vector)), 'constant')
        
        # 결합 벡터 생성 (가중합)
        query_vector = title_padded * 0.3 + summary_padded * 0.3 + claim_padded * 0.4

        # 벡터 확인 및 로깅
        logger.info(f"BERT 쿼리 벡터 L2 노름: {np.linalg.norm(query_vector):.6f}")
        logger.info(f"BERT 쿼리 벡터 샘플 (처음 5개 값): {query_vector[:5]}")

        # 결합 벡터가 모두 0인지 확인
        if np.all(query_vector == 0):
            logger.warning("BERT 쿼리 벡터가 모두 0입니다. 랜덤 벡터로 대체합니다.")
            np.random.seed(42)  # 고정 시드값
            query_vector = np.random.normal(0, 0.1, 1000)
            query_vector = query_vector / np.linalg.norm(query_vector)  # 단위 벡터로 정규화
            logger.info(f"랜덤 벡터로 대체됨: {query_vector[:5]}")
        
        # 벡터 정규화
        query_vector = query_vector.astype(np.float32)
        query_vector = query_vector.reshape(1, -1)  # 2D 배열로 변환
        faiss.normalize_L2(query_vector)
        
        # 정규화 후 다시 확인
        logger.info(f"정규화 후 BERT 쿼리 벡터 L2 노름: {np.linalg.norm(query_vector):.6f}")
        
    except Exception as e:
        logger.error(f"BERT 벡터 처리 중 오류: {str(e)}")
        logger.error(f"TF-IDF 벡터로 폴백합니다.")
        import traceback
        logger.error(traceback.format_exc())
        
        # 오류 발생 시 기존 TF-IDF 방식으로 폴백
        try:
            # 벡터라이저 어휘 확장
            from app.services.vectorizer import update_vectorizer_vocabulary
            update_vectorizer_vocabulary(combined_text)
            
            title_vector = safe_frombuffer(draft["patent_draft_title_tfidf_vector"], target_dim=1000)
            summary_vector = safe_frombuffer(draft["patent_draft_summary_tfidf_vector"], target_dim=1000)
            claim_vector = safe_frombuffer(draft["patent_draft_claim_tfidf_vector"], target_dim=1000)
            
            # 벡터가 없으면 텍스트에서 생성 (fallback)
            if np.all(title_vector == 0) and draft["patent_draft_title"]:
                from app.services.vectorizer import get_tfidf_vector
                title_vector = get_tfidf_vector(draft["patent_draft_title"], update_vocab=True)
            if np.all(summary_vector == 0) and draft["patent_draft_summary"]:
                from app.services.vectorizer import get_tfidf_vector
                summary_vector = get_tfidf_vector(draft["patent_draft_summary"], update_vocab=True)
            if np.all(claim_vector == 0) and draft["patent_draft_claim"]:
                from app.services.vectorizer import get_tfidf_vector
                claim_vector = get_tfidf_vector(draft["patent_draft_claim"], update_vocab=True)
            
            # 결합 벡터 생성
            query_vector = title_vector * 0.3 + summary_vector * 0.3 + claim_vector * 0.4

            # 벡터 확인 및 로깅
            logger.info(f"TF-IDF 쿼리 벡터 L2 노름: {np.linalg.norm(query_vector):.6f}")
            logger.info(f"TF-IDF 쿼리 벡터 샘플 (처음 5개 값): {query_vector[:5]}")

            # 결합 벡터가 모두 0인지 확인
            if np.all(query_vector == 0):
                logger.warning("쿼리 벡터가 모두 0입니다. 랜덤 벡터로 대체합니다.")
                np.random.seed(42)  # 고정 시드값
                query_vector = np.random.normal(0, 0.1, 1000)
                query_vector = query_vector / np.linalg.norm(query_vector)  # 단위 벡터로 정규화
                logger.info(f"랜덤 벡터로 대체됨: {query_vector[:5]}")
            
            # 벡터 정규화
            query_vector = query_vector.astype(np.float32)
            query_vector = query_vector.reshape(1, -1)  # 2D 배열로 변환
            faiss.normalize_L2(query_vector)
            
            # 정규화 후 다시 확인
            logger.info(f"정규화 후 TF-IDF 쿼리 벡터 L2 노름: {np.linalg.norm(query_vector):.6f}")
            
        except Exception as e2:
            logger.error(f"TF-IDF 폴백 처리 중 오류: {str(e2)}")
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

            # 유사도 점수 로깅 (디버깅용)
            logger.info(f"특허 ID {patent_id}의 유사도 점수: {similarity:.4f}")
            
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
    
    # 중복 확인 로직 추가 (여기에 삽입)
    existing_query = """
    SELECT similarity_id FROM similarity 
    WHERE patent_draft_id = :patent_draft_id
    ORDER BY similarity_created_at DESC LIMIT 1
    """
    existing = await database.fetch_one(
        query=existing_query,
        values={"patent_draft_id": patent_draft_id}
    )

    if existing:
        # 먼저 detailed_comparison 테이블의 관련 레코드 삭제
        delete_detailed_query = """
        DELETE FROM detailed_comparison 
        WHERE similarity_patent_id IN (
            SELECT similarity_patent_id FROM similarity_patent 
            WHERE similarity_id = :similarity_id
        )
        """
        await database.execute(
            query=delete_detailed_query,
            values={"similarity_id": existing["similarity_id"]}
        )
        
        # 그 다음 similarity_patent 테이블의 레코드 삭제
        delete_query = """
        DELETE FROM similarity_patent WHERE similarity_id = :similarity_id
        """
        await database.execute(
            query=delete_query,
            values={"similarity_id": existing["similarity_id"]}
        )
        
        # 기존 similarity_id 재사용
        similarity_id = existing["similarity_id"]
        
        # 기존 similarity 레코드 업데이트
        update_query = """
        UPDATE similarity SET similarity_updated_at = :updated_at
        WHERE similarity_id = :similarity_id
        """
        await database.execute(
            query=update_query,
            values={
                "similarity_id": similarity_id,
                "updated_at": now
            }
        )
    else:
        # 새 similarity 레코드 생성
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
        # 저장할 유사도 점수 로깅
        logger.info(f"저장할 유사도 점수: 특허 ID {patent['patent_id']} - 점수: {similarity:.4f}")
        
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

        # 값 매핑 시 반올림 적용하여 정밀도 문제 해결
        score_value = round(float(similarity), 6)
        
        similarity_patent_id = await database.execute(
            query=similarity_patent_query,
            values={
                "similarity_id": similarity_id,
                "patent_id": patent["patent_id"],
                "score": score_value,
                "title_score": score_value,
                "claim_score": score_value,
                "summary_score": score_value,
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
