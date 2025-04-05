from fastapi import APIRouter, BackgroundTasks, HTTPException
from typing import Dict

from app.scripts.train_vectorizer import train_vectorizer_from_patents
from app.services.vectorizer import load_vectorizer

router = APIRouter(prefix="/fastapi/admin", tags=["admin"])

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
