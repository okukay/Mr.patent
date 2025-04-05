from fastapi import Request, HTTPException, Depends, Header
from typing import Optional
import re

# 문서 경로 패턴 정의 (확장)
DOCS_PATHS = [
    "/docs", "/redoc", "/openapi.json",
    "/api/docs", "/api/redoc", "/api/openapi.json",
    "/recommend/docs", "/recommend/redoc", "/recommend/openapi.json"
]

async def verify_token(
    request: Request,
    authorization: Optional[str] = Header(None)
):
    """
    요청에서 인증 토큰을 확인하는 의존성 함수
    문서 경로는 인증 없이 접근 가능하도록 구현
    """
    # 현재 요청 경로
    path = request.url.path
    
    # 문서 관련 경로인지 확인 (더 포괄적인 검사)
    if any(path == doc_path for doc_path in DOCS_PATHS):
        return True
    
    # 경로 끝부분 검사
    if path.endswith("/docs") or path.endswith("/redoc") or path.endswith("/openapi.json"):
        return True
    
    # 정규식으로 문서 패턴 확인 (더 유연한 검사)
    if re.search(r'/(docs|redoc|openapi\.json)/?$', path):
        return True
        
    # 루트 경로도 인증 없이 접근 허용
    if path == "/" or path == "/api" or path == "/api/":
        return True
    
    # 정적 파일 경로 허용 (예: CSS, JS)
    if path.startswith("/static/") or ".js" in path or ".css" in path:
        return True
    
    # 그 외 경로는 토큰 검증
    if not authorization:
        raise HTTPException(
            status_code=401,
            detail={
                "code": "TOKEN_NOT_FOUND",
                "message": "토큰이 존재하지 않습니다.",
                "status": 401
            }
        )
    
    # 토큰 존재하면 통과 (실제 검증 로직은 추가 필요)
    return True
