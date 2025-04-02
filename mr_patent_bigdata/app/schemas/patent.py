from pydantic import BaseModel

class ProcessRequest(BaseModel):
    rtf_directory: str

class ProcessResponse(BaseModel):
    status: bool
    message: str
    task_id: str
    directory: str

class ResumeRequest(BaseModel):
    rtf_directory: str
    start_index: int = 0  # 시작할 특허 인덱스 (기본값: 0)

class ResumeResponse(BaseModel):
    status: bool
    message: str
    task_id: str
    directory: str
