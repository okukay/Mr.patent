from fastapi import APIRouter, HTTPException
from typing import Dict, Any
from datetime import datetime, timezone
import json
import os
import logging
import re

from app.core.database import database
from app.services.kipris import get_patent_public_info, download_patent_pdf, test_kipris_apis, parse_kipris_document
from app.api.routes.ocr import extract_text  # OCR 텍스트 추출 함수 임포트

router = APIRouter(prefix="/fastapi/patent", tags=["patent_public"])
logger = logging.getLogger(__name__)

def get_current_timestamp():
    """현재 시간을 ISO 8601 형식으로 변환 (UTC)"""
    return datetime.now(timezone.utc).isoformat().replace('+00:00', 'Z')

def normalize_application_number(app_number: str) -> str:
    """출원번호를 표준 형식(하이픈 포함)으로 변환"""
    # 공백 및 특수문자 제거
    clean_number = re.sub(r'[^0-9]', '', app_number).strip()
    
    # 길이 확인 및 형식화 (10-XXXX-XXXXXXX)
    if len(clean_number) >= 10:
        return f"{clean_number[:2]}-{clean_number[2:6]}-{clean_number[6:]}"
    return clean_number

@router.get("/kipris/test/{patent_application_number}", response_model=Dict[str, Any])
async def test_kipris_api(patent_application_number: str):
    """KIPRIS API 직접 테스트 (디버깅용)"""
    # 출원번호 정규화 (하이픈 포함 형식으로 변환)
    normalized_number = normalize_application_number(patent_application_number)
    
    # 하이픈 제거된 형식 (API 호출용)
    clean_number = re.sub(r'[^0-9]', '', normalized_number)
    
    # API 테스트 수행
    results = await test_kipris_apis(clean_number)
    
    return {
        "data": {
            "original_number": patent_application_number,
            "normalized_number": normalized_number,
            "clean_number": clean_number,
            "test_results": results
        }
    }

@router.get("/kipris/{patent_application_number}", response_model=Dict[str, Any])
async def get_patent_public(patent_application_number: str):
    """특허 공고전문 정보 조회"""
    try:
        # 출원번호 정규화 (하이픈 포함 형식으로 변환)
        normalized_number = normalize_application_number(patent_application_number)
        logger.info(f"특허 공고전문 정보 조회 시작: 원본={patent_application_number}, 정규화={normalized_number}")
        
        # 이미 저장된 공고전문이 있는지 확인
        check_query = """
        SELECT pp.*, p.patent_application_number 
        FROM patent_public pp
        JOIN patent p ON pp.patent_id = p.patent_id
        WHERE p.patent_application_number = :app_number
        """
        
        patent_public = await database.fetch_one(
            query=check_query,
            values={"app_number": normalized_number}
        )
        
        # 공고전문이 이미 있는 경우
        if patent_public:
            logger.info(f"기존 특허 공고전문 정보 사용: {normalized_number}")
            # API 응답에서 파싱된 데이터 추출
            try:
                api_response = json.loads(patent_public["patent_public_api_response"])
                parsed_data = api_response.get("parsed_data", {})
                raw_content = patent_public["patent_public_content"]
            except json.JSONDecodeError as e:
                logger.warning(f"API 응답 파싱 오류: {str(e)}")
                parsed_data = {}
                raw_content = ""
                
            return {
                "data": {
                    "patent_public_id": patent_public["patent_public_id"],
                    "patent_id": patent_public["patent_id"],
                    "application_number": normalized_number,
                    "raw_content": raw_content,  # 원본 텍스트 추가
                    "parsed_data": parsed_data
                }
            }
        
        # 특허 ID 조회 - 정규화된 출원번호 사용
        patent_query = """
        SELECT patent_id FROM patent 
        WHERE patent_application_number = :app_number
        """
        
        patent = await database.fetch_one(
            query=patent_query,
            values={"app_number": normalized_number}
        )
        
        # 특허 ID 검색 로그
        logger.info(f"특허 ID 조회 결과: 출원번호={normalized_number}, 결과={'찾음' if patent else '찾지 못함'}")
        
        if not patent:
            logger.warning(f"특허 정보를 찾을 수 없음: {normalized_number}")
            return {
                "status": False,
                "code": 404,
                "data": None,
                "error": {
                    "code": "PATENT_NOT_FOUND",
                    "message": "해당 출원번호의 특허를 찾을 수 없습니다."
                }
            }
        
        # KIPRIS API를 통해 공고전문 정보 가져오기 - 하이픈 제거된 형식 사용
        clean_number = str(re.sub(r'[^0-9]', '', normalized_number))  # 문자열 타입 보장
        logger.info(f"KIPRIS API 호출 시작: 출원번호={clean_number} (타입: {type(clean_number)})")
        
        # 디버깅을 위한 직접 API 테스트
        test_results = await test_kipris_apis(clean_number)
        logger.info(f"API 테스트 결과: {json.dumps(test_results, ensure_ascii=False)[:500]}")
        
        patent_info = await get_patent_public_info(clean_number)
        
        if not patent_info:
            logger.warning(f"KIPRIS에서 특허 정보를 찾을 수 없음: {clean_number}")
            return {
                "status": False,
                "code": 404,
                "data": None,
                "error": {
                    "code": "KIPRIS_DATA_NOT_FOUND",
                    "message": "KIPRIS에서 해당 특허 정보를 찾을 수 없습니다."
                }
            }
        
        logger.info(f"KIPRIS API 호출 성공: {clean_number}, 공고번호: {patent_info.get('publication_number', '알 수 없음')}")
        
        # PDF 다운로드 및 텍스트 추출 (OCR + 파싱)
        logger.info(f"PDF 다운로드 시작: {clean_number}")
        pdf_path, pdf_name = await download_patent_pdf(clean_number)
        
        if not pdf_path:
            logger.error(f"PDF 다운로드 실패: {clean_number}")
            return {
                "status": False,
                "code": 500,
                "data": None,
                "error": {
                    "code": "PDF_DOWNLOAD_FAILED",
                    "message": "특허 공고전문 PDF 다운로드에 실패했습니다."
                }
            }
        
        logger.info(f"PDF 다운로드 성공: {pdf_path}")
        
        # OCR 처리 및 문서 파싱 
        logger.info(f"PDF 텍스트 추출 및 파싱 시작: {pdf_path}")
        
        # OCR로 텍스트만 추출 (ocr.py의 extract_text 함수 사용)
        pdf_content = await extract_text(pdf_path)
        logger.info(f"PDF 텍스트 추출 완료: {len(pdf_content)} 글자")
        
        # 새로 추가한 함수로 파싱 (접두사 없는 필드명 사용)
        parsed_data = parse_kipris_document(pdf_content)
        logger.info(f"특허 문서 파싱 완료: {len(parsed_data)} 섹션")
        
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
        
        logger.info(f"특허 공고전문 DB 저장 시작: {clean_number}")
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
        logger.info(f"특허 공고전문 DB 저장 완료: {patent_public_id}")
        
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
                "application_number": normalized_number,
                "raw_content": pdf_content,  # 원본 텍스트 추가
                "parsed_data": parsed_data
            }
        }
        
    except Exception as e:
        # 오류 발생 시 임시 파일 정리
        try:
            if 'pdf_path' in locals() and pdf_path and os.path.exists(pdf_path):
                os.remove(pdf_path)
        except:
            pass
            
        logger.error(f"특허 공고전문 처리 중 오류: {str(e)}")
        # 스택 트레이스 로깅
        import traceback
        logger.error(traceback.format_exc())
        
        return {
            "status": False,
            "code": 500,
            "data": None,
            "error": {
                "code": "SERVER_ERROR",
                "message": f"서버 오류가 발생했습니다: {str(e)}"
            }
        }
