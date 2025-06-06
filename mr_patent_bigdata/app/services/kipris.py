import httpx
import os
import xml.etree.ElementTree as ET
from typing import Dict, Any, Tuple, Optional, List
import tempfile
import logging
import json
import re
import urllib.parse  # URL 인코딩을 위해 추가

from app.core.config import settings
from app.api.routes.ocr import extract_text, parse_patent_document

logger = logging.getLogger(__name__)

# KIPRIS API 서비스 키 (URL 인코딩 적용)
KIPRIS_SERVICE_KEY = settings.kipris_service_key

# 캐싱을 위한 딕셔너리
_patent_info_cache = {}
_patent_pdf_cache = {}

async def get_patent_info_and_pdf(application_number: str) -> Tuple[Dict[str, Any], str, str]:
    """특허 정보 조회와 PDF 다운로드를 통합한 함수"""
    
    # 캐싱: 이미 조회한 정보가 있으면 재사용
    cache_key = str(application_number)
    if cache_key in _patent_info_cache and cache_key in _patent_pdf_cache:
        logger.info(f"캐시에서 특허 정보 및 PDF 정보 로드: {application_number}")
        return _patent_info_cache[cache_key], _patent_pdf_cache[cache_key][0], _patent_pdf_cache[cache_key][1]
    
    # 공고전문 API만 호출 (getAnnFullTextInfoSearch)
    endpoint = "getAnnFullTextInfoSearch"
    description = "공고전문"
    
    # API 호출 및 파싱
    try:
        url = f"http://plus.kipris.or.kr/kipo-api/kipi/patUtiModInfoSearchSevice/{endpoint}"
        
        params = {
            "applicationNumber": application_number,
            "ServiceKey": KIPRIS_SERVICE_KEY
        }
        
        headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
            "Accept": "application/xml, text/xml, */*",
            "Accept-Language": "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7"
        }
        
        logger.info(f"KIPRIS {description} API 통합 호출: {application_number} ({endpoint})")
        
        async with httpx.AsyncClient() as client:
            response = await client.get(url, params=params, headers=headers, timeout=30.0)
            
            if response.status_code != 200:
                logger.error(f"KIPRIS API 오류: 상태코드 {response.status_code}")
                return None, "", ""
            
            try:
                # XML 응답 파싱
                root = ET.fromstring(response.text)
                
                # 성공 여부 확인
                success_yn = root.find(".//successYN")
                result_code = root.find(".//resultCode")
                
                success = (success_yn is not None and success_yn.text == "Y") or \
                          (result_code is not None and result_code.text == "00")
                
                if not success:
                    result_msg = root.find(".//resultMsg")
                    msg = result_msg.text if result_msg is not None else "알 수 없는 오류"
                    logger.error(f"KIPRIS API 응답 오류: {msg}")
                    return None, "", ""
                    
                # 문서 정보 추출
                item = root.find(".//item")
                if item is None:
                    logger.error(f"KIPRIS API 응답에 문서 정보가 없습니다")
                    return None, "", ""
                    
                doc_name = item.find("docName")
                path = item.find("path")
                
                if doc_name is None or path is None:
                    logger.error(f"KIPRIS API 응답에 필수 정보가 누락되었습니다")
                    return None, "", ""
                    
                # 특허 정보 생성
                publication_number = doc_name.text.replace(".pdf", "").replace(".PDF", "")
                patent_info = {
                    "publication_number": publication_number,
                    "doc_name": doc_name.text,
                    "path": path.text,
                    "type": description
                }
                
                # 정보 캐시에 저장
                _patent_info_cache[cache_key] = patent_info
                
                # PDF 다운로드 처리
                pdf_url = path.text
                pdf_name = doc_name.text
                
                logger.info(f"특허 PDF 다운로드 시작: {pdf_name}")
                
                # 임시 폴더에 PDF 저장
                temp_dir = tempfile.gettempdir()
                local_path = os.path.join(temp_dir, pdf_name)
                
                # PDF 다운로드
                async with httpx.AsyncClient() as pdf_client:
                    pdf_response = await pdf_client.get(pdf_url, headers=headers, timeout=60.0)
                    
                    if pdf_response.status_code != 200:
                        logger.error(f"PDF 다운로드 오류: 상태코드 {pdf_response.status_code}")
                        return patent_info, "", ""
                    
                    # 파일 저장
                    with open(local_path, "wb") as f:
                        f.write(pdf_response.content)
                    
                    logger.info(f"PDF 다운로드 완료: {local_path}, 크기: {len(pdf_response.content)} 바이트")
                    
                    # PDF 정보 캐시에 저장
                    _patent_pdf_cache[cache_key] = (local_path, pdf_name)
                    
                    return patent_info, local_path, pdf_name
                    
            except ET.ParseError as e:
                logger.error(f"KIPRIS API 응답 XML 파싱 오류: {str(e)}")
                return None, "", ""
                
    except Exception as e:
        logger.error(f"KIPRIS API 통합 호출 중 오류: {str(e)}")
        import traceback
        logger.error(traceback.format_exc())
        return None, "", ""

async def test_kipris_apis(application_number: str) -> Dict[str, Any]:
    """모든 KIPRIS API 직접 테스트 (디버깅용)"""
    results = {}
    
    # 출원번호 타입 확인
    results["input"] = {
        "application_number": application_number,
        "type": str(type(application_number))
    }
    
    # 출원번호에서 하이픈 제거 - 명시적으로 문자열 유지
    clean_number = str(re.sub(r'[^0-9]', '', application_number)).strip()
    results["clean_number"] = {
        "value": clean_number,
        "type": str(type(clean_number))
    }
    
    # API 키 로깅 (보안상 일부만 표시)
    service_key_masked = KIPRIS_SERVICE_KEY[:5] + "..." + KIPRIS_SERVICE_KEY[-5:] if len(KIPRIS_SERVICE_KEY) > 10 else "***"
    results["service_key_info"] = {
        "masked": service_key_masked,
        "length": len(KIPRIS_SERVICE_KEY)
    }
    
    # 시도할 API 엔드포인트 목록
    endpoints = [
        "getAnnFullTextInfoSearch",    # 공고전문 API
        "getPubFullTextInfoSearch"     # 공개전문 API
    ]
    
    for endpoint in endpoints:
        url = f"http://plus.kipris.or.kr/kipo-api/kipi/patUtiModInfoSearchSevice/{endpoint}"
        
        # 서비스 키 URL 인코딩 적용
        params = {
            "applicationNumber": application_number,
            "ServiceKey": KIPRIS_SERVICE_KEY  # 인코딩 없이 원본 키 사용
        }
        
        # 브라우저처럼 보이는 User-Agent 추가
        headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
            "Accept": "application/xml, text/xml, */*",
            "Accept-Language": "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7"
        }
        
        try:
            endpoint_result = {
                "endpoint": endpoint,
                "url": f"{url}?applicationNumber={application_number}",
                "headers": headers
            }
            
            async with httpx.AsyncClient() as client:
                response = await client.get(url, params=params, headers=headers, timeout=30.0)
                
                endpoint_result["status_code"] = response.status_code
                endpoint_result["response_headers"] = dict(response.headers)
                
                if response.status_code == 200:
                    try:
                        # XML 응답 파싱 시도
                        root = ET.fromstring(response.text)
                        
                        # 주요 필드 추출
                        success_yn = root.find(".//successYN")
                        result_code = root.find(".//resultCode")
                        result_msg = root.find(".//resultMsg")
                        
                        endpoint_result["parsed"] = {
                            "successYN": success_yn.text if success_yn is not None else None,
                            "resultCode": result_code.text if result_code is not None else None,
                            "resultMsg": result_msg.text if result_msg is not None else None
                        }
                        
                        # 문서 정보 추출
                        item = root.find(".//item")
                        if item is not None:
                            doc_name = item.find("docName")
                            path = item.find("path")
                            
                            endpoint_result["document"] = {
                                "docName": doc_name.text if doc_name is not None else None,
                                "path": path.text if path is not None else None
                            }
                    except ET.ParseError:
                        endpoint_result["error"] = "XML 파싱 오류"
                        endpoint_result["raw_response"] = response.text[:1000]  # 앞부분만 기록
                
                results[endpoint] = endpoint_result
                
        except Exception as e:
            results[endpoint] = {
                "error": str(e),
                "traceback": str(logging.traceback.format_exc())
            }
    
    return results

# 기존 함수들 캐싱 적용
async def get_patent_public_info(application_number: str, only_ann=False) -> Optional[Dict[str, str]]:
    """KIPRIS API를 통해 특허 공고전문 정보를 가져옵니다."""
    
    # 캐시 확인
    cache_key = str(application_number)
    if cache_key in _patent_info_cache:
        logger.info(f"캐시에서 특허 정보 로드: {application_number}")
        return _patent_info_cache[cache_key]
    
    if not isinstance(application_number, str):
        application_number = str(application_number)
    
    logger.info(f"KIPRIS API 호출 준비: {application_number} (타입: {type(application_number)})")
    
    # 공고전문 API만 호출
    endpoint = "getAnnFullTextInfoSearch"
    description = "공고전문"
    
    # 공개전문까지 시도하는 기존 로직은 only_ann=False인 경우에만 실행
    endpoints = [(endpoint, description)]
    if not only_ann:
        endpoints.append(("getPubFullTextInfoSearch", "공개전문"))
    
    for endpoint, description in endpoints:
        url = f"http://plus.kipris.or.kr/kipo-api/kipi/patUtiModInfoSearchSevice/{endpoint}"
        
        # 서비스 키 URL 인코딩 적용
        params = {
            "applicationNumber": application_number,
            "ServiceKey": KIPRIS_SERVICE_KEY  # 인코딩 없이 원본 키 사용
        }
        
        # 브라우저처럼 보이는 User-Agent 추가 (중요!)
        headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
            "Accept": "application/xml, text/xml, */*",
            "Accept-Language": "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7"
        }
        
        try:
            logger.info(f"KIPRIS {description} API 시도: {application_number} ({endpoint})")
            
            async with httpx.AsyncClient() as client:
                response = await client.get(url, params=params, headers=headers, timeout=30.0)
                
                if response.status_code != 200:
                    logger.error(f"KIPRIS API 오류: 상태코드 {response.status_code}")
                    continue
                
                # 응답 내용 로깅 (필요시만 활성화)
                logger.debug(f"KIPRIS API 응답 처음 100자: {response.text[:100]}")
                
                try:
                    # XML 응답 파싱
                    root = ET.fromstring(response.text)
                    
                    # successYN 또는 resultCode로 성공 여부 확인 (둘 다 체크)
                    success_yn = root.find(".//successYN")
                    result_code = root.find(".//resultCode")
                    
                    # 성공 여부 확인 로직 개선
                    success = (success_yn is not None and success_yn.text == "Y") or \
                              (result_code is not None and result_code.text == "00")
                    
                    if not success:
                        result_msg = root.find(".//resultMsg")
                        msg = result_msg.text if result_msg is not None else "알 수 없는 오류"
                        logger.error(f"KIPRIS API 응답 오류: 코드={result_code.text if result_code is not None else '없음'}, 메시지={msg}")
                        continue
                        
                    # 문서 정보 추출
                    item = root.find(".//item")
                    if item is None:
                        logger.error(f"KIPRIS API 응답에 문서 정보가 없습니다: {description}")
                        continue
                        
                    doc_name = item.find("docName")
                    path = item.find("path")
                    
                    if doc_name is None or path is None:
                        logger.error(f"KIPRIS API 응답에 필수 정보가 누락되었습니다: {description}")
                        continue
                        
                    # 공개/공고번호 추출 (파일명에서 .pdf 또는 .PDF 제거)
                    publication_number = doc_name.text.replace(".pdf", "").replace(".PDF", "")
                    
                    logger.info(f"특허 {description} 정보 조회 성공: {publication_number}")
                        
                    return {
                        "publication_number": publication_number,
                        "doc_name": doc_name.text,
                        "path": path.text,
                        "type": description
                    }
                    
                except ET.ParseError as e:
                    logger.error(f"KIPRIS API 응답 XML 파싱 오류: {str(e)}")
                    logger.error(f"받은 응답: {response.text[:200]}...")
                    continue
                    
        except Exception as e:
            logger.error(f"KIPRIS {description} API 호출 중 오류 발생: {str(e)}")
            # 스택 트레이스 로깅 추가
            import traceback
            logger.error(traceback.format_exc())
            continue
    
    # 모든 API 시도 후에도 실패
    logger.error(f"모든 KIPRIS API 시도 실패: {application_number}")
    return None

async def download_patent_pdf(application_number: str, info=None) -> Tuple[str, str]:
    """KIPRIS API를 통해 특허 공고전문 PDF를 다운로드합니다."""
    
    # 캐시 확인
    cache_key = str(application_number)
    if cache_key in _patent_pdf_cache:
        logger.info(f"캐시에서 PDF 정보 로드: {application_number}")
        return _patent_pdf_cache[cache_key]
    
    # 정보가 전달되지 않은 경우 통합 함수 사용
    if not info:
        patent_info, pdf_path, pdf_name = await get_patent_info_and_pdf(application_number)
        if pdf_path:
            return pdf_path, pdf_name
        
    pdf_url = info["path"]
    pdf_name = info["doc_name"]
    
    logger.info(f"특허 PDF 다운로드 시작: {pdf_name}, URL: {pdf_url}")
    
    # 임시 폴더에 PDF 저장
    temp_dir = tempfile.gettempdir()
    local_path = os.path.join(temp_dir, pdf_name)
    
    try:
        # User-Agent 헤더 추가 (중요)
        headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
        }
        
        async with httpx.AsyncClient() as client:
            response = await client.get(pdf_url, headers=headers, timeout=60.0)
            
            if response.status_code != 200:
                logger.error(f"PDF 다운로드 오류: 상태코드 {response.status_code}")
                return "", ""
            
            # 응답 헤더 로깅
            logger.debug(f"PDF 다운로드 응답 헤더: {response.headers}")
            
            # 응답이 실제로 PDF 형식인지 확인
            content_type = response.headers.get("content-type", "")
            if "application/pdf" not in content_type and not content_type.startswith("application/octet-stream"):
                logger.warning(f"PDF 다운로드 응답이 PDF 형식이 아닙니다: {content_type}")
                
            # 파일 크기 확인
            content_length = len(response.content)
            if content_length < 1000:  # 너무 작으면 유효한 PDF가 아닐 수 있음
                logger.warning(f"PDF 파일 크기가 너무 작습니다: {content_length} 바이트")
                
            # 파일 저장
            with open(local_path, "wb") as f:
                f.write(response.content)
            
            logger.info(f"PDF 다운로드 완료: {local_path}, 크기: {content_length} 바이트")
                
            return local_path, pdf_name
                
    except Exception as e:
        logger.error(f"PDF 다운로드 중 오류 발생: {str(e)}")
        return "", ""

async def extract_text_from_pdf(pdf_path: str) -> Tuple[str, Dict]:
    """PDF 파일에서 텍스트를 추출하고 문서 구조를 파싱합니다."""
    try:
        # PDF 파일 존재 확인
        if not os.path.exists(pdf_path):
            logger.error(f"PDF 파일이 존재하지 않습니다: {pdf_path}")
            return "PDF 파일을 찾을 수 없습니다", {}
            
        # 파일 크기 확인
        file_size = os.path.getsize(pdf_path)
        logger.info(f"PDF 파일 크기: {file_size} 바이트")
        
        if file_size < 1000:
            logger.warning(f"PDF 파일 크기가 너무 작습니다: {file_size} 바이트")
        
        # OCR 처리로 텍스트 추출
        logger.info(f"PDF 파일 OCR 처리 시작: {pdf_path}")
        ocr_text = extract_text(pdf_path)
        
        # 추출된 텍스트 확인
        text_length = len(ocr_text)
        logger.info(f"PDF 파일 OCR 처리 완료: {text_length} 글자 추출")
        
        if text_length < 100:
            logger.warning(f"OCR 추출 텍스트가 너무 짧습니다: {ocr_text[:100]}")
        
        # 특허 문서 구조 파싱
        logger.info("특허 문서 섹션 파싱 시작")
        parsed_data = parse_patent_document(ocr_text)
        
        # 파싱 결과 확인
        section_count = len(parsed_data)
        logger.info(f"특허 문서 섹션 파싱 완료: {section_count}개 섹션 추출")
        
        return ocr_text, parsed_data
    except Exception as e:
        logger.error(f"PDF 텍스트 추출 중 오류 발생: {str(e)}")
        # 스택 트레이스 로깅
        import traceback
        logger.error(traceback.format_exc())
        return "텍스트 추출 실패", {}

def parse_kipris_document(ocr_text: str) -> Dict[str, str]:
    """KIPRIS 공고전문 전용 파싱 함수 - 접두사 없는 깔끔한 필드명 사용"""
    # OCR 결과에서 페이지 구분자 제거
    ocr_text = re.sub(r'---\s*페이지\s*\d+\s*---', '', ocr_text)

    def extract_section(text, start_marker, end_marker):
        try:
            start_idx = text.find(start_marker)
            if start_idx == -1:
                # 다른 형태의 마커 시도 (청구항의 경우)
                if start_marker == "청구항":
                    alt_markers = ["청구항 1.", "【청구항 1】"]
                    for alt_marker in alt_markers:
                        start_idx = text.find(alt_marker)
                        if start_idx != -1:
                            start_idx = start_idx
                            break
                if start_idx == -1:
                    return ""
            
            # 시작 마커 이후로 이동
            start_idx = start_idx + len(start_marker)
            
            # 끝 마커가 있는 경우
            if end_marker and end_marker in text[start_idx:]:
                end_idx = text.find(end_marker, start_idx)
                return text[start_idx:end_idx].strip()
            else:
                # 끝 마커가 없는 경우 (예: 마지막 섹션)
                return text[start_idx:].strip()
        except Exception as e:
            logger.warning(f"섹션 추출 오류 ({start_marker} ~ {end_marker}): {str(e)}")
            return ""

    return {
        "title": extract_section(ocr_text, "발명의 명칭", "요약"),
        "technical_field": extract_section(ocr_text, "기술분야", "배경기술"),
        "background": extract_section(ocr_text, "배경기술", "발명의 내용"),
        "problem": extract_section(ocr_text, "해결하려는 과제", "과제의 해결 수단"),
        "solution": extract_section(ocr_text, "과제의 해결 수단", "발명의 효과"),
        "effect": extract_section(ocr_text, "발명의 효과", "도면의 간단한 설명"),
        "detailed": extract_section(ocr_text, "발명을 실시하기 위한 구체적인 내용", "청구범위"),
        "summary": extract_section(ocr_text, "요약", "명세서"),
        "claim": extract_section(ocr_text, "청구항", "발명의 설명")
    }
