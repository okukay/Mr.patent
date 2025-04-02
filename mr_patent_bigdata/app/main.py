from fastapi import FastAPI
from app.core.config import settings
from app.core.database import database
from app.api.routes import patent, task
from app.core.logging import logger

app = FastAPI(title=settings.PROJECT_NAME)

# 라우터 등록
app.include_router(patent.router)
app.include_router(task.router)

# 이벤트 핸들러
@app.on_event("startup")
async def startup():
    await database.connect()
    logger.info("데이터베이스 연결 성공")

@app.on_event("shutdown")
async def shutdown():
    await database.disconnect()
    logger.info("데이터베이스 연결 종료")
