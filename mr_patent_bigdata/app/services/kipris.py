import httpx
import os
import xml.etree.ElementTree as ET
from typing import Dict, Any, Tuple, Optional
import tempfile
import logging
import json

from app.core.config import settings
from app.api.routes.ocr import extract_text, parse_patent_document

logger = logging.getLogger(__name__)

# KIPRIS API 서비스 키
KIPRIS_SERVICE_KEY = settings.kipris_service_key

async def get_patent_public_info(application_number: str) -> Optional[Dict[str, str]]:
    """KIPRIS API를 통해 특허 공고전문 정보를 가져옵니다."""
    
    url = "http://plus.kipris.or.kr/kipo-api/kipi/patUtiModInfoSearchSevice/getStandardAnnFullTextInfoSearch"
    
    params = {
        "applicationNumber": application_number,
        "ServiceKey": KIPRIS_SERVICE_KEY
    }
    
    try:
        async with httpx.AsyncClient() as client:
            response = await client.get(url, params=params)
            
            if response.status_code != 200:
                logger.error(f"KIPRIS API 오류: 상태코드 {response.status_code}")
                return None
                
            # XML 응답 파싱
            root = ET.fromstring(response.text)
            
            # 성공 여부 확인
            success_yn = root.find(".//successYN")
            if success_yn is None or success_yn.text != "Y":
                logger.error(f"KIPRIS API 응답 오류: {response.text}")
                return None
                
            # 문서 정보 추출
            item = root.find(".//item")
            if item is None:
                logger.error("KIPRIS API 응답에 문서 정보가 없습니다.")
                return None
                
            doc_name = item.find("docName")
            path = item.find("path")
            
            if doc_name is None or path is None:
                logger.error("KIPRIS API 응답에 필수 정보가 누락되었습니다.")
                return None
                
            # 공고번호 추출 (파일명에서 .PDF 제거)
            publication_number = doc_name.text.replace(".PDF", "")
                
            return {
                "publication_number": publication_number,
                "doc_name": doc_name.text,
                "path": path.text
            }
                
    except Exception as e:
        logger.error(f"KIPRIS API 호출 중 오류 발생: {str(e)}")
        return None

async def download_patent_pdf(application_number: str) -> Tuple[str, str]:
    """KIPRIS API를 통해 특허 공고전문 PDF를 다운로드합니다."""
    
    # 먼저 PDF 정보 얻기
    info = await get_patent_public_info(application_number)
    
    if not info:
        logger.error(f"특허 공고전문 정보를 찾을 수 없습니다: {application_number}")
        return "", ""
        
    pdf_url = info["path"]
    pdf_name = info["doc_name"]
    
    # 임시 폴더에 PDF 저장
    temp_dir = tempfile.gettempdir()
    local_path = os.path.join(temp_dir, pdf_name)
    
    try:
        async with httpx.AsyncClient() as client:
            response = await client.get(pdf_url)
            
            if response.status_code != 200:
                logger.error(f"PDF 다운로드 오류: 상태코드 {response.status_code}")
                return "", ""
                
            # 파일 저장
            with open(local_path, "wb") as f:
                f.write(response.content)
                
            return local_path, pdf_name
                
    except Exception as e:
        logger.error(f"PDF 다운로드 중 오류 발생: {str(e)}")
        return "", ""

async def extract_text_from_pdf(pdf_path: str) -> Tuple[str, Dict]:
    """PDF 파일에서 텍스트를 추출하고 문서 구조를 파싱합니다."""
    try:
        # OCR 처리로 텍스트 추출
        logger.info(f"PDF 파일 OCR 처리 시작: {pdf_path}")
        ocr_text = extract_text(pdf_path)
        logger.info(f"PDF 파일 OCR 처리 완료: {len(ocr_text)} 글자 추출")
        
        # 특허 문서 구조 파싱
        logger.info("특허 문서 섹션 파싱 시작")
        parsed_data = parse_patent_document(ocr_text)
        logger.info("특허 문서 섹션 파싱 완료")
        
        return ocr_text, parsed_data
    except Exception as e:
        logger.error(f"PDF 텍스트 추출 중 오류 발생: {str(e)}")
        return "텍스트 추출 실패", {}
