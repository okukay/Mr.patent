from fastapi import APIRouter, BackgroundTasks, HTTPException
from typing import Dict, Any
import logging

from app.scripts.train_vectorizer import train_vectorizer_from_patents
from app.services.vectorizer import load_vectorizer
from app.services.spark_vectorizer import process_patents_with_spark
from app.core.database import database

# 통합된 라우터
router = APIRouter(prefix="/fastapi/admin", tags=["admin"])
logger = logging.getLogger(__name__)

# 벡터라이저 관련 API (원래 admin.py)
@router.post("/train-vectorizer", response_model=Dict[str, str])
async def train_vectorizer(background_tasks: BackgroundTasks):
    """TF-IDF 벡터라이저를 특허 데이터로 학습 (백그라운드)"""
    background_tasks.add_task(train_vectorizer_from_patents)
    
    return {"message": "TF-IDF 벡터라이저 학습이 백그라운드에서 시작되었습니다."}

@router.get("/load-vectorizer", response_model=Dict[str, str])
async def load_tfidf_vectorizer():
    """저장된 TF-IDF 벡터라이저 로드"""
    try:
        vectorizer = load_vectorizer()
        return {"message": f"TF-IDF 벡터라이저 로드 완료 (어휘 크기: {len(vectorizer.vocabulary_)})"}
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"벡터라이저 로드 실패: {str(e)}"
        )

@router.post("/rebuild-vectors", response_model=Dict[str, Any])
async def rebuild_patent_vectors(background_tasks: BackgroundTasks, with_bert: bool = False):
    """모든 특허의 벡터 데이터를 재생성합니다"""
    try:
        # 1. 데이터베이스에서 모든 특허 원본 데이터 로드
        query = """
        SELECT patent_id, patent_title, patent_summary, patent_claim, 
               patent_application_number, patent_ipc
        FROM patent
        """
        
        all_patents = await database.fetch_all(query)
        patent_count = len(all_patents)
        logger.info(f"벡터 재생성 시작: 총 {patent_count}개 특허")
        
        # 2. 백그라운드 작업으로 벡터 재생성 시작
        background_tasks.add_task(
            process_patents_with_spark,
            all_patents=all_patents,
            with_bert=with_bert
        )
        
        return {
            "success": True,
            "data": {
                "message": f"벡터 재생성이 시작되었습니다. 총 {patent_count}개 특허 처리 예정",
                "patent_count": patent_count,
                "with_bert": with_bert,
                "note": "상세 진행 상황은 서버 로그에서 확인하세요."
            }
        }
    
    except Exception as e:
        logger.error(f"벡터 재생성 시작 중 오류: {str(e)}")
        import traceback
        logger.error(traceback.format_exc())
        
        return {
            "success": False,
            "error": {
                "message": "벡터 재생성 실패",
                "detail": str(e)
            }
        }
