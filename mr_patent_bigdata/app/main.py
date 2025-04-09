from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.core.config import settings
from app.core.database import database
from app.api.routes import patent, task, folders, drafts, similarity, admin, patent_public, comparison, ocr
from app.core.logging import logger
from app.services.vectorizer import load_vectorizer

API_TITLE = "특허 관리 API"
API_DESCRIPTION = "특허 처리 및 관리를 위한 API 문서"

app = FastAPI(
    title="특허 관리 API",
    version="1.0.0",
    description="API 문서"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 프로덕션에서는 실제 도메인으로 제한해야 함
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 라우터 등록
app.include_router(patent.router)
app.include_router(task.router)
app.include_router(folders.router)
app.include_router(drafts.router)
app.include_router(similarity.router)
app.include_router(admin.router)
app.include_router(patent_public.router)
app.include_router(comparison.router)
app.include_router(ocr.router)

# 이벤트 핸들러
@app.on_event("startup")
async def startup():
    await database.connect()
    logger.info("데이터베이스 연결 성공")

    # 벡터라이저 상태 확인
    from app.services.vectorizer import check_vectorizer_status, train_and_save_vectorizer
    status = check_vectorizer_status()
    
    # 문제가 있으면 긴급 재학습
    if status.startswith("empty") or status.startswith("not"):
        logger.warning("벡터라이저 문제 감지: 긴급 재학습 시작...")
        
        # 데이터베이스에서 100개 샘플 추출해 학습
        sample_query = """
        SELECT patent_title, patent_summary, patent_claim
        FROM patent LIMIT 100
        """
        samples = await database.fetch_all(query=sample_query)
        
        texts = []
        for s in samples:
            combined = f"{s['patent_title']} {s['patent_summary']} {s['patent_claim']}"
            if combined.strip():
                texts.append(combined)
        
        if texts:
            train_and_save_vectorizer(texts)
            logger.info("벡터라이저 긴급 재학습 완료")

@app.on_event("shutdown")
async def shutdown():
    await database.disconnect()
    logger.info("데이터베이스 연결 종료")

@app.get("/")
async def root():
    return {"message": "특허 관리 API가 실행 중입니다."}
