from fastapi import Request, HTTPException, Depends, Header
from typing import Optional, Dict, Any
import re
import logging

# 로깅 설정 - 디버깅용
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("auth_middleware")

# 문서 경로 패턴 정의 (확장)
DOCS_PATHS = [
    "/docs", "/redoc", "/openapi.json",
    "/api/docs", "/api/redoc", "/api/openapi.json",
    "/recommend/docs", "/recommend/redoc", "/recommend/openapi.json"
]

# 인증 예외 경로 패턴 (정규식)
AUTH_EXEMPT_PATTERNS = [
    r'^/docs/?$',
    r'^/redoc/?$',
    r'^/openapi\.json/?$',
    r'^/api/docs/?$', 
    r'^/api/redoc/?$',
    r'^/api/openapi\.json/?$',
    r'^/$',
    r'^/api/?$',
    r'^/static/.*$',
    r'.*\.js$',
    r'.*\.css$',
    r'.*\.png$',
    r'.*swagger-ui.*$',
    r'.*favicon\.ico$'
]

async def verify_token(
    request: Request,
    authorization: Optional[str] = Header(None)
) -> Dict[str, Any]:
    """
    요청에서 인증 토큰을 확인하는 의존성 함수
    문서 경로는 인증 없이 접근 가능하도록 구현
    """
    path = request.url.path
    method = request.method
    
    # 디버깅용 로그
    logger.info(f"Path: {path}, Method: {method}")
    
    # 1. 직접 경로 매칭
    if path in DOCS_PATHS:
        logger.info(f"직접 경로 매칭 성공: {path}")
        return {"authenticated": False, "path": path, "exempt_reason": "docs_path"}
    
    # 2. 경로 끝부분 매칭
    if path.endswith(("/docs", "/redoc", "/openapi.json")):
        logger.info(f"경로 끝부분 매칭 성공: {path}")
        return {"authenticated": False, "path": path, "exempt_reason": "path_suffix"}
    
    # 3. 정규식 패턴 매칭
    for pattern in AUTH_EXEMPT_PATTERNS:
        if re.match(pattern, path):
            logger.info(f"정규식 패턴 매칭 성공: {path} -> {pattern}")
            return {"authenticated": False, "path": path, "exempt_reason": "pattern_match"}
    
    # 디버깅용: 옵션 요청은 항상 허용
    if method == "OPTIONS":
        return {"authenticated": False, "path": path, "exempt_reason": "options_method"}
    
    # 인증이 필요한 경로이나 토큰 없음
    if not authorization:
        logger.warning(f"인증 토큰 없음: {path}")
        raise HTTPException(
            status_code=401,
            detail={
                "code": "TOKEN_NOT_FOUND",
                "message": "토큰이 존재하지 않습니다.",
                "status": 401
            }
        )
    
    # 토큰 검증 - 여기서는 토큰이 존재하면 통과
    logger.info(f"토큰 검증 성공: {path}")
    return {"authenticated": True, "path": path, "token": authorization}
