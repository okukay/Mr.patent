import os
from elasticsearch import Elasticsearch
import logging

logger = logging.getLogger(__name__)

# ElasticSearch 연결 설정 - Docker 네트워크 내에서 컨테이너 이름으로 접근
ELASTICSEARCH_HOST = "http://mr_patent_elasticsearch:9200"

# SSL 사용 여부 확인 (Elasticsearch 8.x는 기본적으로 보안 활성화)
ELASTICSEARCH_USE_SSL = True
ELASTICSEARCH_VERIFY_CERTS = False  # 자체 서명 인증서 사용 시 False

# 기본 인증 정보
ELASTICSEARCH_USERNAME = os.getenv("ELASTICSEARCH_USERNAME", "elastic")
ELASTICSEARCH_PASSWORD = os.getenv("ELASTICSEARCH_PASSWORD", "")  # 필요시 채워넣기

# 클라이언트 인스턴스 생성
def get_elasticsearch_client():
    try:
        # 보안 설정 활성화된 경우
        if ELASTICSEARCH_USE_SSL:
            return Elasticsearch(
                ELASTICSEARCH_HOST,
                basic_auth=(ELASTICSEARCH_USERNAME, ELASTICSEARCH_PASSWORD),
                verify_certs=ELASTICSEARCH_VERIFY_CERTS
            )
        # 보안 설정 비활성화된 경우
        else:
            return Elasticsearch(ELASTICSEARCH_HOST)
    except Exception as e:
        logger.error(f"ElasticSearch 연결 오류: {str(e)}")
        return None

# 인덱스 생성 함수
async def create_patent_index(client=None):
    if client is None:
        client = get_elasticsearch_client()
        
    if not client:
        logger.error("ElasticSearch 클라이언트 생성 실패")
        return False
    
    index_name = "patents"
    
    # 인덱스가 이미 존재하는지 확인
    if client.indices.exists(index=index_name):
        logger.info(f"인덱스 '{index_name}'가 이미 존재합니다.")
        return True
    
    # 인덱스 설정 및 매핑
    settings = {
        "settings": {
            "number_of_shards": 3,
            "number_of_replicas": 1
        },
        "mappings": {
            "properties": {
                "patent_id": {"type": "long"},
                "cluster_id": {"type": "integer"},
                "title": {"type": "text"},
                "summary": {"type": "text"},
                "claim": {"type": "text"},
                "combined_vector": {
                    "type": "dense_vector",
                    "dims": 100,
                    "index": True,
                    "similarity": "cosine"
                }
            }
        }
    }
    
    try:
        # 인덱스 생성
        client.indices.create(index=index_name, body=settings)
        logger.info(f"인덱스 '{index_name}' 생성 완료")
        return True
    except Exception as e:
        logger.error(f"인덱스 생성 오류: {str(e)}")
        return False
