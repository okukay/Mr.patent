from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.openapi.docs import get_swagger_ui_html, get_redoc_html
from fastapi.openapi.utils import get_openapi
from fastapi.responses import JSONResponse

from app.core.config import settings
from app.core.database import database
from app.api.routes import patent, task, folders, drafts, similarity, admin, patent_public, comparison, reports, ocr
from app.core.logging import logger
from app.services.vectorizer import load_vectorizer

# API 문서 커스텀 제목 및 설명
API_TITLE = "특허 관리 API"
API_DESCRIPTION = "특허 처리 및 관리를 위한 API 문서"

# 앱 설정 - 명시적인 문서 경로 설정
app = FastAPI(
    title=settings.PROJECT_NAME,
    docs_url="/docs",
    redoc_url="/redoc",
    openapi_url="/openapi.json"
)

# 커스텀 OpenAPI 스키마 생성 함수
def custom_openapi():
    if app.openapi_schema:
        return app.openapi_schema
    
    openapi_schema = get_openapi(
        title=API_TITLE,
        version="1.0.0",
        description=API_DESCRIPTION,
        routes=app.routes,
    )
    
    # 절대 URL 사용
    openapi_schema["servers"] = [
        {"url": "https://j12d208.p.ssafy.io/api", "description": "API Server"}
    ]
    
    app.openapi_schema = openapi_schema
    return app.openapi_schema

# 커스텀 OpenAPI 스키마 적용
app.openapi = custom_openapi

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
        logger.error(f"벡터라이저 로드 실패: {e}")

@app.on_event("shutdown")
async def shutdown():
    await database.disconnect()
    logger.info("데이터베이스 연결 종료")

@app.get("/")
async def root():
    return {"message": "특허 관리 API가 실행 중입니다."}
