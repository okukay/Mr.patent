from fastapi import APIRouter, HTTPException, status

from app.core.database import database, task_status
from app.schemas.task import TaskResponse

router = APIRouter(prefix="/fastapi/patents", tags=["tasks"])

@router.get("/process-status/{task_id}", response_model=TaskResponse)
async def get_process_status(task_id: str):
    """작업 진행 상태 확인"""
    query = task_status.select().where(task_status.c.task_id == task_id)
    result = await database.fetch_one(query)
    
    if not result:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="작업 ID를 찾을 수 없습니다."
        )
    
    return {
        "status": True,
        "data": {
            "task_id": result["task_id"],
            "status": result["status"],
            "progress": result["progress"],
            "total_files": result["total_files"],
            "processed_files": result["processed_files"],
            "created_at": result["created_at"],
            "updated_at": result["updated_at"]
        }
    }
