from fastapi import APIRouter, HTTPException
from typing import Dict, Any
from datetime import datetime, timezone
import json
import os
import logging

from app.core.database import database
from app.services.kipris import get_patent_public_info, download_patent_pdf, extract_text_from_pdf

router = APIRouter(prefix="/fastapi/patent", tags=["patent_public"])
logger = logging.getLogger(__name__)

def get_current_timestamp():
    """현재 시간을 ISO 8601 형식으로 변환 (UTC)"""
    return datetime.now(timezone.utc).isoformat().replace('+00:00', 'Z')

@router.get("/kipris/{patent_application_number}", response_model=Dict[str, Any])
async def get_patent_public(patent_application_number: str):
    """특허 공고전문 정보 조회"""
    try:
        # 이미 저장된 공고전문이 있는지 확인
        check_query = """
        SELECT pp.*, p.patent_application_number 
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
            # API 응답에서 파싱된 데이터 추출
            try:
                api_response = json.loads(patent_public["patent_public_api_response"])
                parsed_data = api_response.get("parsed_data", {})
            except:
                parsed_data = {}
                
            return {
                "data": {
                    "patent_public_id": patent_public["patent_public_id"],
                    "patent_id": patent_public["patent_id"],
                    "application_number": patent_application_number,
                    "parsed_data": parsed_data
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
            return {
                "status": False,
                "code": 404,
                "data": None,
                "error": {
                    "code": "PATENT_NOT_FOUND",
                    "message": "해당 출원번호의 특허를 찾을 수 없습니다."
                },
                "timestamp": get_current_timestamp()
            }
        
        # KIPRIS API를 통해 공고전문 정보 가져오기
        patent_info = await get_patent_public_info(patent_application_number)
        
        if not patent_info:
            return {
                "status": False,
                "code": 404,
                "data": None,
                "error": {
                    "code": "KIPRIS_DATA_NOT_FOUND",
                    "message": "KIPRIS에서 해당 특허 정보를 찾을 수 없습니다."
                },
                "timestamp": get_current_timestamp()
            }
        
        # PDF 다운로드 및 텍스트 추출 (OCR + 파싱)
        pdf_path, pdf_name = await download_patent_pdf(patent_application_number)
        
        if not pdf_path:
            return {
                "status": False,
                "code": 500,
                "data": None,
                "error": {
                    "code": "PDF_DOWNLOAD_FAILED",
                    "message": "특허 공고전문 PDF 다운로드에 실패했습니다."
                },
                "timestamp": get_current_timestamp()
            }
        
        # OCR 처리 및 문서 파싱 
        pdf_content, parsed_data = await extract_text_from_pdf(pdf_path)
        
        # DB에 저장
        now = datetime.now(timezone.utc)
        insert_query = """
        INSERT INTO patent_public (
            patent_id,
            patent_public_number,
            patent_public_content,
            patent_public_api_response,
            patent_public_created_at,
            patent_public_updated_at
        ) VALUES (
            :patent_id,
            :public_number,
            :content,
            :api_response,
            :created_at,
            :updated_at
        )
        """
        
        # API 응답 데이터 구성
        api_response_data = {
            "kipris_info": patent_info,
            "parsed_data": parsed_data
        }
        
        patent_public_id = await database.execute(
            query=insert_query,
            values={
                "patent_id": patent["patent_id"],
                "public_number": patent_info["publication_number"],
                "content": pdf_content,
                "api_response": json.dumps(api_response_data, ensure_ascii=False),
                "created_at": now,
                "updated_at": now
            }
        )
        
        # 텍스트 추출 완료 후 임시 PDF 파일 삭제
        try:
            os.remove(pdf_path)
            logger.info(f"임시 PDF 파일 삭제: {pdf_path}")
        except Exception as e:
            logger.warning(f"임시 PDF 파일 삭제 실패: {str(e)}")
        
        return {
            "data": {
                "patent_public_id": patent_public_id,
                "patent_id": patent["patent_id"],
                "application_number": patent_application_number,
                "parsed_data": parsed_data
            },
            "timestamp": get_current_timestamp()
        }
        
    except Exception as e:
        # 오류 발생 시 임시 파일 정리
        try:
            if 'pdf_path' in locals() and pdf_path and os.path.exists(pdf_path):
                os.remove(pdf_path)
        except:
            pass
            
        logger.error(f"특허 공고전문 처리 중 오류: {str(e)}")
        return {
            "status": False,
            "code": 500,
            "data": None,
            "error": {
                "code": "SERVER_ERROR",
                "message": f"서버 오류가 발생했습니다: {str(e)}"
            },
            "timestamp": get_current_timestamp()
        }
