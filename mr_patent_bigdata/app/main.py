from fastapi import FastAPI, Depends
from fastapi.middleware.base import BaseHTTPMiddleware
from fastapi.middleware.cors import CORSMiddleware
from fastapi.openapi.docs import get_swagger_ui_html, get_redoc_html
from fastapi.openapi.utils import get_openapi

from app.core.config import settings
from app.core.database import database
from app.api.routes import patent, task, folders, drafts, similarity, admin, patent_public, comparison, reports, ocr
from app.core.logging import logger
from app.services.vectorizer import load_vectorizer
from app.api.dependencies import verify_token

# API 문서 커스텀 제목 및 설명
API_TITLE = "특허 관리 API"
API_DESCRIPTION = "특허 처리 및 관리를 위한 API 문서"

# 미들웨어 클래스 정의
class AuthenticationMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request, call_next):
        # 문서 경로는 인증에서 제외
        if request.url.path.endswith("/docs") or request.url.path.endswith("/openapi.json") or request.url.path.endswith("/redoc"):
            return await call_next(request)
            
        # 기존 요청 처리
        return await call_next(request)

app = FastAPI(
    title=settings.PROJECT_NAME,
    docs_url=None,  # 기본 /docs 경로 비활성화
    redoc_url=None  # 기본 /redoc 경로 비활성화
)

# 미들웨어 추가
app.add_middleware(AuthenticationMiddleware)

# CORS 설정 (안드로이드 앱에서 접근 허용)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 프로덕션에서는 실제 도메인으로 제한해야 함
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 라우터 등록 (인증 의존성 적용)
app.include_router(patent.router, dependencies=[Depends(verify_token)])
app.include_router(task.router, dependencies=[Depends(verify_token)])
app.include_router(folders.router, dependencies=[Depends(verify_token)])
app.include_router(drafts.router, dependencies=[Depends(verify_token)])
app.include_router(similarity.router, dependencies=[Depends(verify_token)])
app.include_router(admin.router, dependencies=[Depends(verify_token)])
app.include_router(patent_public.router, dependencies=[Depends(verify_token)])
app.include_router(comparison.router, dependencies=[Depends(verify_token)])
app.include_router(reports.router, dependencies=[Depends(verify_token)])
app.include_router(ocr.router, dependencies=[Depends(verify_token)])

# 커스텀 문서 경로 설정 (인증 없이 접근 가능)
@app.get("/docs", include_in_schema=False)
async def custom_swagger_ui_html():
    return get_swagger_ui_html(
        openapi_url="/openapi.json",
        title=API_TITLE,
        swagger_js_url="https://cdn.jsdelivr.net/npm/swagger-ui-dist@5.9.0/swagger-ui-bundle.js",
        swagger_css_url="https://cdn.jsdelivr.net/npm/swagger-ui-dist@5.9.0/swagger-ui.css",
    )

@app.get("/api/docs", include_in_schema=False)
async def api_swagger_ui_html():
    return get_swagger_ui_html(
        openapi_url="/openapi.json",
        title=API_TITLE,
        swagger_js_url="https://cdn.jsdelivr.net/npm/swagger-ui-dist@5.9.0/swagger-ui-bundle.js",
        swagger_css_url="https://cdn.jsdelivr.net/npm/swagger-ui-dist@5.9.0/swagger-ui.css",
    )

@app.get("/redoc", include_in_schema=False)
async def redoc_html():
    return get_redoc_html(
        openapi_url="/openapi.json",
        title=API_TITLE,
        redoc_js_url="https://cdn.jsdelivr.net/npm/redoc@next/bundles/redoc.standalone.js"
    )

@app.get("/api/redoc", include_in_schema=False)
async def api_redoc_html():
    return get_redoc_html(
        openapi_url="/openapi.json",
        title=API_TITLE,
        redoc_js_url="https://cdn.jsdelivr.net/npm/redoc@next/bundles/redoc.standalone.js"
    )

@app.get("/openapi.json", include_in_schema=False)
async def get_open_api_endpoint():
    return get_openapi(
        title=API_TITLE,
        version="1.0.0",
        description=API_DESCRIPTION,
        routes=app.routes,
    )

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

@app.get("/", dependencies=[])
async def root():
    return {"message": "특허 관리 API가 실행 중입니다."}
