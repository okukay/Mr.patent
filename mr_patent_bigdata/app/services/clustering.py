import numpy as np
from sklearn.cluster import MiniBatchKMeans
from sklearn.decomposition import PCA
import json
from tqdm import tqdm
import logging
import datetime
import pickle
import os
from app.core.database import database

logger = logging.getLogger(__name__)

# safe_frombuffer 함수 추가 (similarity.py에서 가져옴)
def safe_frombuffer(buffer, target_dim=1000, dtype=np.float32):
    """안전하게 버퍼를 배열로 변환하고 차원을 맞춰주는 함수"""
    if buffer is None or len(buffer) == 0:
        return np.zeros(target_dim, dtype=dtype)
    
    try:
        # 버퍼를 배열로 변환
        vec = np.frombuffer(buffer, dtype=dtype)
        
        # NaN 값 처리
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

# 벡터 로드 함수 추가 - JSON과 바이너리 형식 모두 지원
def load_vector(vector_data, default_dim=1000):
    """다양한 형식의 벡터 데이터를 안전하게 로드"""
    if vector_data is None or len(vector_data) == 0:
        return np.zeros(default_dim)
    
    # 바이너리 데이터로 먼저 시도
    try:
        return safe_frombuffer(vector_data, target_dim=default_dim)
    except:
        # JSON 파싱 시도
        try:
            return np.array(json.loads(vector_data))
        except:
            # 모든 방법 실패 시 기본 영벡터 반환
            return np.zeros(default_dim)

async def create_patent_clusters(n_clusters=100):
    """특허 데이터를 K-means로 군집화합니다"""
    logger.info(f"특허 데이터 군집화 시작: {n_clusters}개 클러스터 생성")
    
    # 1. 데이터베이스에서 TF-IDF 벡터 로드
    patent_vectors = []
    patent_ids = []
    
    # 배치 크기 감소 (메모리 사용량 최적화)
    batch_size = 1000  # 5000에서 1000으로 감소
    offset = 0
    total_patents = 0
    
    while True:
        query = """
        SELECT patent_id, patent_title_tfidf_vector, 
               patent_summary_tfidf_vector, patent_claim_tfidf_vector
        FROM patent
        LIMIT :limit OFFSET :offset
        """
        
        patents = await database.fetch_all(
            query=query, 
            values={"limit": batch_size, "offset": offset}
        )
        
        if not patents:
            break
            
        for patent in patents:
            try:
                # 수정된 벡터 로딩 방식 (바이너리 또는 JSON 지원)
                title_vector = load_vector(patent["patent_title_tfidf_vector"])
                summary_vector = load_vector(patent["patent_summary_tfidf_vector"])
                claim_vector = load_vector(patent["patent_claim_tfidf_vector"])
                
                # 유효성 검사 추가
                if (len(title_vector) > 0 and len(summary_vector) > 0 and len(claim_vector) > 0 and
                    not np.all(title_vector == 0) and not np.all(summary_vector == 0) and not np.all(claim_vector == 0)):
                    
                    # 가중치 적용 (필요에 따라 조정)
                    combined_vector = title_vector * 0.3 + summary_vector * 0.3 + claim_vector * 0.4
                    
                    patent_vectors.append(combined_vector)
                    patent_ids.append(patent["patent_id"])
                else:
                    logger.warning(f"특허 ID {patent['patent_id']}: 유효하지 않은 벡터 데이터 (영벡터)")
            except Exception as e:
                logger.error(f"특허 ID {patent['patent_id']} 벡터 처리 중 오류: {str(e)}")
        
        total_patents += len(patents)
        logger.info(f"데이터 로딩 진행: {len(patent_ids)}개 특허 로드됨 (총 처리: {total_patents}개)")
        offset += batch_size
    
    # 2. 차원 축소로 성능 향상 (100차원으로 축소)
    logger.info(f"PCA 차원 축소 시작: {len(patent_vectors)}개 벡터")
    
    # float32로 변환하여 메모리 사용량 감소
    patent_vectors = np.array(patent_vectors, dtype=np.float32)
    
    pca = PCA(n_components=100)
    reduced_vectors = pca.fit_transform(patent_vectors)
    
    # PCA 모델 저장 (나중에 재사용)
    os.makedirs("models", exist_ok=True)
    with open("models/patent_pca_model.pkl", "wb") as f:
        pickle.dump(pca, f)
    
    logger.info("PCA 차원 축소 완료: 100차원으로 축소됨 (모델 저장됨)")
    
    # 3. 미니배치 K-means 군집화 수행 (메모리 효율적)
    logger.info(f"K-means 군집화 시작: {len(reduced_vectors)}개 특허, {n_clusters}개 클러스터")
    kmeans = MiniBatchKMeans(
        n_clusters=n_clusters, 
        random_state=42,
        batch_size=1000,
        max_iter=100
    )
    cluster_labels = kmeans.fit_predict(reduced_vectors)
    
    # K-means 모델 저장
    with open("models/patent_kmeans_model.pkl", "wb") as f:
        pickle.dump(kmeans, f)
    
    cluster_centers = kmeans.cluster_centers_
    logger.info("K-means 군집화 완료 (모델 저장됨)")
    
    # 4. 기존 클러스터 정보 삭제
    await database.execute("TRUNCATE TABLE cluster")
    logger.info("기존 클러스터 정보 삭제 완료")
    
    # 5. 데이터베이스에 클러스터 결과 저장
    # 클러스터 마스터 테이블 생성
    for i in range(n_clusters):
        query = """
        INSERT INTO cluster (cluster_id, cluster_center_vector, cluster_size)
        VALUES (:cluster_id, :center_vector, :size)
        """
        
        # 해당 클러스터에 속한 특허 수
        cluster_size = np.sum(cluster_labels == i)
        
        await database.execute(
            query=query,
            values={
                "cluster_id": i,
                "center_vector": json.dumps(cluster_centers[i].tolist()),
                "size": int(cluster_size)
            }
        )
    logger.info(f"클러스터 정보 저장 완료: {n_clusters}개 클러스터")
    
    # 6. 각 특허의 클러스터 할당 저장 (배치 처리로 최적화)
    batch_size = 500  # 1000에서 500으로 감소 (안정성)
    for i in range(0, len(patent_ids), batch_size):
        batch_ids = patent_ids[i:i+batch_size]
        batch_labels = cluster_labels[i:i+batch_size]
        
        # 배치 업데이트 쿼리 구성
        for j in range(len(batch_ids)):
            query = """
            UPDATE patent 
            SET cluster_id = :cluster_id
            WHERE patent_id = :patent_id
            """
            
            await database.execute(
                query=query,
                values={
                    "cluster_id": int(batch_labels[j]),
                    "patent_id": batch_ids[j]
                }
            )
        
        logger.info(f"특허 클러스터 할당 진행: {i+len(batch_ids)}/{len(patent_ids)}")
    
    logger.info(f"군집화 완료: {len(patent_ids)}개 특허를 {n_clusters}개 클러스터로 분류")
    
    # 7. ElasticSearch 인덱싱 (선택적)
    try:
        from app.services.elastic_config import create_patent_index, get_elasticsearch_client
        
        es_client = get_elasticsearch_client()
        if es_client and await create_patent_index(es_client):
            logger.info("ElasticSearch에 특허 데이터 인덱싱 시작")
            
            # ElasticSearch 인덱싱 프로세스 시작
            index_result = await index_patents_to_elasticsearch(pca)
            logger.info(f"ElasticSearch 인덱싱 결과: {index_result}")
        else:
            logger.warning("ElasticSearch 연결 실패 - 인덱싱 건너뜀")
    except Exception as e:
        logger.error(f"ElasticSearch 인덱싱 오류: {str(e)}")
    
    return {
        "cluster_count": n_clusters,
        "patent_count": len(patent_ids),
        "clusters": [int(np.sum(cluster_labels == i)) for i in range(n_clusters)]
    }

async def index_patents_to_elasticsearch(pca=None):
    """군집화된 특허 데이터를 ElasticSearch에 인덱싱합니다"""
    from app.services.elastic_config import get_elasticsearch_client
    
    logger.info("ElasticSearch 인덱싱 시작")
    start_time = datetime.datetime.now()
    
    # ElasticSearch 클라이언트 가져오기
    es_client = get_elasticsearch_client()
    if not es_client:
        logger.error("ElasticSearch 클라이언트 생성 실패")
        return {"status": "error", "message": "ElasticSearch 연결 실패"}
    
    # 인덱스 존재 여부 확인 (이미 create_patent_index에서 확인했으므로 생략 가능)
    index_name = "patents"
    
    try:
        # 배치 처리를 위한 변수 설정
        batch_size = 500  # 1000에서 500으로 감소
        offset = 0
        indexed_count = 0
        failed_count = 0
        
        # 전체 특허 수 확인
        total_count = await database.fetch_val(
            query="SELECT COUNT(*) FROM patent WHERE cluster_id IS NOT NULL"
        )
        logger.info(f"인덱싱할 총 특허 수: {total_count}")
        
        while True:
            # 클러스터 ID가 있는 특허만 가져오기
            query = """
            SELECT p.patent_id, p.patent_title, p.patent_summary, p.patent_claim, 
                   p.patent_title_tfidf_vector, p.patent_summary_tfidf_vector, p.patent_claim_tfidf_vector,
                   p.cluster_id
            FROM patent p
            WHERE p.cluster_id IS NOT NULL
            LIMIT :limit OFFSET :offset
            """
            
            patents = await database.fetch_all(
                query=query, 
                values={"limit": batch_size, "offset": offset}
            )
            
            if not patents:
                break
            
            # 벌크 인덱싱을 위한 데이터 준비
            bulk_data = []
            
            for patent in patents:
                try:
                    # 수정된 벡터 로딩 방식
                    title_vector = load_vector(patent["patent_title_tfidf_vector"])
                    summary_vector = load_vector(patent["patent_summary_tfidf_vector"])
                    claim_vector = load_vector(patent["patent_claim_tfidf_vector"])
                    
                    # 결합 벡터 생성 (가중치 적용)
                    combined_vector = title_vector * 0.3 + summary_vector * 0.3 + claim_vector * 0.4
                    
                    # PCA로 차원 축소 (제공된 경우)
                    if pca is not None:
                        reduced_vector = pca.transform([combined_vector])[0]
                    else:
                        # PCA 없을 경우, 앞 100개 차원만 사용 (필요에 따라 조정)
                        reduced_vector = combined_vector[:100]
                    
                    # 인덱스 작업 항목 추가
                    bulk_data.append(
                        {"index": {"_index": index_name, "_id": str(patent["patent_id"])}}
                    )
                    
                    # 문서 데이터 추가
                    bulk_data.append({
                        "patent_id": patent["patent_id"],
                        "cluster_id": patent["cluster_id"],
                        "title": patent["patent_title"],
                        "summary": patent["patent_summary"],
                        "claim": patent["patent_claim"],
                        "combined_vector": reduced_vector.tolist()
                    })
                    
                except Exception as e:
                    logger.error(f"특허 ID {patent['patent_id']} 처리 중 오류: {str(e)}")
                    failed_count += 1
            
            # 벌크 인덱싱 실행
            if bulk_data:
                try:
                    # 벌크 요청 크기 제한 (기본 100MB)
                    bulk_response = es_client.bulk(body=bulk_data, refresh=True)
                    
                    # 응답 확인
                    if bulk_response.get("errors", False):
                        error_items = [item for item in bulk_response.get("items", []) 
                                      if item.get("index", {}).get("error")]
                        logger.error(f"벌크 인덱싱 중 {len(error_items)}개 항목 오류 발생")
                        failed_count += len(error_items)
                    
                    # 성공한 문서 수 계산 (2개 항목이 1개 문서에 해당)
                    successful_docs = len(bulk_data) // 2 - len(error_items) if 'error_items' in locals() else len(bulk_data) // 2
                    indexed_count += successful_docs
                    
                except Exception as e:
                    logger.error(f"벌크 인덱싱 중 오류: {str(e)}")
                    failed_count += len(bulk_data) // 2
            
            # 진행 상황 로깅
            offset += batch_size
            if indexed_count % 10000 == 0 and indexed_count > 0:
                logger.info(f"인덱싱 진행: {indexed_count}/{total_count} 특허 처리됨 ({indexed_count/total_count*100:.1f}%)")
        
        # 완료 시간 계산
        end_time = datetime.datetime.now()
        duration = (end_time - start_time).total_seconds()
        
        # 결과 반환
        logger.info(f"ElasticSearch 인덱싱 완료: {indexed_count}개 성공, {failed_count}개 실패, {duration:.2f}초 소요")
        
        return {
            "status": "success",
            "indexed_count": indexed_count,
            "failed_count": failed_count,
            "duration_seconds": duration
        }
        
    except Exception as e:
        logger.error(f"ElasticSearch 인덱싱 프로세스 중 오류: {str(e)}")
        import traceback
        logger.error(traceback.format_exc())
        
        return {
            "status": "error",
            "message": f"ElasticSearch 인덱싱 실패: {str(e)}"
        }
