from fastapi import Request, HTTPException, Depends, Header
from typing import Optional

# 문서 경로 패턴 정의
DOCS_PATHS = ["/docs", "/redoc", "/openapi.json"]

async def verify_token(
    request: Request,
    authorization: Optional[str] = Header(None)
):
    """
    요청에서 인증 토큰을 확인하는 의존성 함수
    문서 경로는 인증 없이 접근 가능하도록 구현
    """
    # 문서 경로는 인증 없이 접근 허용
    if request.url.path in DOCS_PATHS or request.url.path.endswith("/docs") or request.url.path.endswith("/redoc"):
        return True
        
    # 루트 경로도 인증 없이 접근 허용
    if request.url.path == "/":
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
    
    # 여기에 실제 토큰 검증 로직을 추가할 수 있습니다
    # 예: JWT 디코딩, DB에서 토큰 확인 등
    
    return True
