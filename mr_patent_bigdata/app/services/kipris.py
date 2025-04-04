import os
import aiohttp
import xml.etree.ElementTree as ET
from typing import Dict, Tuple, Any, Optional

# KIPRIS API 키 및 기본 URL
KIPRIS_API_KEY = os.environ.get("KIPRIS_API_KEY", "your_api_key")
KIPRIS_BASE_URL = "http://plus.kipris.or.kr/kipo-api/kipi/patUtiModInfoSearchSevice"

# PDF 저장 경로
PDF_DIR = "patent_pdfs"
os.makedirs(PDF_DIR, exist_ok=True)

async def get_patent_public_info(application_number: str) -> Optional[Dict[str, Any]]:
    """KIPRIS API를 사용해 특허 공고전문 정보 가져오기"""
    url = f"{KIPRIS_BASE_URL}/getStandardAnnFullTextInfoSearch"
    params = {
        "applicationNumber": application_number,
        "ServiceKey": KIPRIS_API_KEY
    }
    
    try:
        async with aiohttp.ClientSession() as session:
            async with session.get(url, params=params) as response:
                if response.status == 200:
                    text = await response.text()
                    # XML 응답 파싱
                    root = ET.fromstring(text)
                    
                    # 응답 상태 확인
                    header = root.find(".//header")
                    if header is not None and header.find("successYN").text == "Y":
                        item = root.find(".//item")
                        if item is not None:
                            doc_name = item.find("docName").text
                            pdf_path = item.find("path").text
                            
                            return {
                                "publication_number": doc_name.replace(".PDF", ""),
                                "doc_name": doc_name,
                                "pdf_path": pdf_path,
                                "application_number": application_number,
                                "raw_response": text
                            }
                return None
    except Exception as e:
        print(f"KIPRIS API 호출 중 오류: {str(e)}")
        return None

async def download_patent_pdf(publication_number: str) -> Tuple[str, str]:
    """특허 공고전문 PDF 다운로드"""
    try:
        # 먼저 공고전문 정보 가져오기 (publication_number를 application_number로 간주)
        patent_info = await get_patent_public_info(publication_number)
        
        if not patent_info or "pdf_path" not in patent_info:
            # 정보가 없거나 PDF 경로가 없는 경우 빈 파일 생성
            pdf_name = f"{publication_number}.pdf"
            pdf_path = os.path.join(PDF_DIR, pdf_name)
            with open(pdf_path, 'w') as f:
                f.write("")
            return pdf_path, pdf_name
        
        # API 응답에서 가져온 PDF 경로와 파일명 사용
        pdf_url = patent_info["pdf_path"]
        pdf_name = patent_info["doc_name"]
        pdf_path = os.path.join(PDF_DIR, pdf_name)
        
        # PDF 다운로드
        async with aiohttp.ClientSession() as session:
            async with session.get(pdf_url) as response:
                if response.status == 200:
                    with open(pdf_path, 'wb') as f:
                        f.write(await response.read())
                    return pdf_path, pdf_name
                else:
                    # 오류 시 빈 파일 생성
                    with open(pdf_path, 'w') as f:
                        f.write("")
                    return pdf_path, pdf_name
                    
    except Exception as e:
        print(f"PDF 다운로드 중 오류: {str(e)}")
        # 오류 시 빈 파일 생성
        pdf_name = f"{publication_number}.pdf"
        pdf_path = os.path.join(PDF_DIR, pdf_name)
        with open(pdf_path, 'w') as f:
            f.write("")
        return pdf_path, pdf_name
