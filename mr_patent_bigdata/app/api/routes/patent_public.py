from fastapi import APIRouter, HTTPException, Response
from typing import Dict, Any
from datetime import datetime, timezone

from app.core.database import database
from app.services.kipris import get_patent_public_info, download_patent_pdf

router = APIRouter(prefix="/api/patent", tags=["patent_public"])

def get_current_timestamp():
    """현재 시간을 ISO 8601 형식으로 변환 (UTC)"""
    return datetime.now(timezone.utc).isoformat().replace('+00:00', 'Z')

@router.get("/kipris/{patent_application_number}", response_model=Dict[str, Any])
async def get_patent_public(patent_application_number: str):
    """특허 공고전문 정보 조회"""
    # 이미 저장된 공고전문이 있는지 확인
    check_query = """
    SELECT pp.*, p.patent_id 
    FROM patent_public pp
    JOIN patent p ON pp.patent_id = p.patent_id
    WHERE p.patent_application_number = :app_number
    """
    
    patent_public = await database.fetch_one(
        query=check_query,
        values={"app_number": patent_application_number}
    )
    
    # 공고전문이 이미 있는 경우
    if patent_public:
        return {
            "data": {
                "patent_public_id": patent_public["patent_public_id"],
                "patent_id": patent_public["patent_id"],
                "application_number": patent_application_number,
                "pdf_path": patent_public["patent_public_pdf_path"],
                "pdf_name": patent_public["patent_public_pdf_name"],
                "is_processed": bool(patent_public["patent_public_is_processed"])
            },
            "timestamp": get_current_timestamp()
        }
    
    # 특허 ID 조회
    patent_query = """
    SELECT patent_id FROM patent 
    WHERE patent_application_number = :app_number
    """
    
    patent = await database.fetch_one(
        query=patent_query,
        values={"app_number": patent_application_number}
    )
    
    if not patent:
        raise HTTPException(
            status_code=404,
            detail={
                "code": "PATENT_NOT_FOUND",
                "message": "해당 출원번호의 특허를 찾을 수 없습니다.",
                "timestamp": get_current_timestamp()
            }
        )
    
    # KIPRIS API를 통해 공고전문 정보 가져오기
    patent_info = await get_patent_public_info(patent_application_number)
    
    if not patent_info:
        raise HTTPException(
            status_code=404,
            detail={
                "code": "KIPRIS_DATA_NOT_FOUND",
                "message": "KIPRIS에서 해당 특허 정보를 찾을 수 없습니다.",
                "timestamp": get_current_timestamp()
            }
        )
    
    # PDF 다운로드
    pdf_path, pdf_name = await download_patent_pdf(patent_info["publication_number"])
    
    # DB에 저장
    now = datetime.now(timezone.utc)
    insert_query = """
    INSERT INTO patent_public (
        patent_id,
        patent_public_number,
        patent_public_pdf_path,
        patent_public_pdf_name,
        patent_public_content,
        patent_public_api_response,
        patent_public_is_processed,
        patent_public_retrieved_at,
        patent_public_created_at,
        patent_public_updated_at
    ) VALUES (
        :patent_id,
        :public_number,
        :pdf_path,
        :pdf_name,
        :content,
        :api_response,
        :is_processed,
        :retrieved_at,
        :created_at,
        :updated_at
    )
    """
    
    # 간단한 내용 예시 (실제로는 PDF 텍스트 추출 필요)
    content = f"특허 공고전문 내용 (출원번호: {patent_application_number})"
    
    patent_public_id = await database.execute(
        query=insert_query,
        values={
            "patent_id": patent["patent_id"],
            "public_number": patent_info["publication_number"],
            "pdf_path": pdf_path,
            "pdf_name": pdf_name,
            "content": content,
            "api_response": str(patent_info),
            "is_processed": 1,
            "retrieved_at": now,
            "created_at": now,
            "updated_at": now
        }
    )
    
    return {
        "data": {
            "patent_public_id": patent_public_id,
            "patent_id": patent["patent_id"],
            "application_number": patent_application_number,
            "pdf_path": pdf_path,
            "pdf_name": pdf_name,
            "is_processed": True
        },
        "timestamp": get_current_timestamp()
    }
