from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.core.config import settings
from app.core.database import database
from app.api.routes import patent, task, folders, drafts, similarity, admin, patent_public, comparison, reports, ocr
from app.core.logging import logger
from app.services.vectorizer import load_vectorizer

app = FastAPI(title=settings.PROJECT_NAME)

# CORS 설정 (안드로이드 앱에서 접근 허용)
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
app.include_router(reports.router)
app.include_router(ocr.router)

# 이벤트 핸들러
@app.on_event("startup")
async def startup():
    await database.connect()
    logger.info("데이터베이스 연결 성공")

    # 벡터라이저 로드
    try:
        load_vectorizer()
    except Exception as e:
        print(f"벡터라이저 로드 실패: {e}")

@app.on_event("shutdown")
async def shutdown():
    await database.disconnect()
    logger.info("데이터베이스 연결 종료")

@app.get("/")
async def root():
    return {"message": "특허 관리 API가 실행 중입니다."}
