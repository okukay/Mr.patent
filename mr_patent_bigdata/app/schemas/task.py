from pydantic import BaseModel
from datetime import datetime
from typing import Optional

class TaskStatusData(BaseModel):
    task_id: str
    status: str
    progress: int
    total_files: int
    processed_files: int
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None

class TaskResponse(BaseModel):
    status: bool
    data: Optional[TaskStatusData] = None
