from fastapi import APIRouter, UploadFile, File, HTTPException
from typing import Dict, Any
import os
import io
import re
from google.cloud import vision
from pdf2image import convert_from_path
from app.core.logging import logger
from dotenv import load_dotenv

router = APIRouter(prefix="/fastapi", tags=["ocr"])

# 임시 파일 저장 경로
TEMP_DIR = "temp_pdf"
os.makedirs(TEMP_DIR, exist_ok=True)

# .env 파일 로드
load_dotenv()

# 환경 변수에서 경로 가져오기
credentials_path = os.getenv("GOOGLE_CREDENTIALS_PATH")
if credentials_path:
    os.environ["GOOGLE_APPLICATION_CREDENTIALS"] = credentials_path
else:
    logger.error("GOOGLE_CREDENTIALS_PATH 환경 변수가 설정되지 않았습니다.")

def parse_patent_document(ocr_text: str) -> Dict[str, str]:
    # OCR 결과에서 페이지 구분자 제거
    ocr_text = re.sub(r'---\s*페이지\s*\d+\s*---', '', ocr_text)

    # 결과를 저장할 딕셔너리
    patent_draft = {
        "patent_draft_title": "",
        "patent_draft_technical_field": "",
        "patent_draft_background": "",
        "patent_draft_problem": "",
        "patent_draft_solution": "",
        "patent_draft_effect": "",
        "patent_draft_detailed": "",
        "patent_draft_summary": "",
        "patent_draft_claim": ""
    }
    
    # 발명의 명칭 추출 (이 부분은 정상 작동)
    title_pattern = r"발명의\s*명칭\s*(?::|：|\s)\s*(.*?)(?:\n|$)"
    title_match = re.search(title_pattern, ocr_text, re.DOTALL)
    if title_match:
        patent_draft["patent_draft_title"] = title_match.group(1).strip()
    
    # 기술분야 추출 (개선된 패턴)
    tech_field_pattern = r"(?:【기술분야】|기\s*술\s*분\s*야)\s*(.*?)(?:【배경기술】|배\s*경\s*기\s*술)"
    tech_field_match = re.search(tech_field_pattern, ocr_text, re.DOTALL | re.IGNORECASE)
    if tech_field_match:
        patent_draft["patent_draft_technical_field"] = tech_field_match.group(1).strip()
    
    # 배경기술 추출 (개선된 패턴)
    background_pattern = r"(?:【배경기술】|배\s*경\s*기\s*술)\s*(.*?)(?:【해결하려는\s*과제】|해결\s*하\s*려는\s*과제)"
    background_match = re.search(background_pattern, ocr_text, re.DOTALL | re.IGNORECASE)
    if background_match:
        patent_draft["patent_draft_background"] = background_match.group(1).strip()
    
    # 해결하려는 과제(문제점) 추출 (개선된 패턴)
    problem_pattern = r"(?:【해결하려는\s*과제】|해결\s*하\s*려는\s*과제)\s*(.*?)(?:【과제의\s*해결\s*수단】|과제의\s*해결\s*수단)"
    problem_match = re.search(problem_pattern, ocr_text, re.DOTALL | re.IGNORECASE)
    if problem_match:
        patent_draft["patent_draft_problem"] = problem_match.group(1).strip()
    
    # 과제의 해결 수단(해결방법) 추출 (개선된 패턴)
    solution_pattern = r"(?:【과제의\s*해결\s*수단】|과제의\s*해결\s*수단)\s*(.*?)(?:【발명의\s*효과】|발명의\s*효과)"
    solution_match = re.search(solution_pattern, ocr_text, re.DOTALL | re.IGNORECASE)
    if solution_match:
        patent_draft["patent_draft_solution"] = solution_match.group(1).strip()
    
    # 발명의 효과 추출 (개선된 패턴)
    effect_pattern = r"(?:【발명의\s*효과】|발명의\s*효과)\s*(.*?)(?:【청구항\s*1】|청구항\s*1|제\s*1\s*항)"
    effect_match = re.search(effect_pattern, ocr_text, re.DOTALL | re.IGNORECASE)
    if effect_match:
        patent_draft["patent_draft_effect"] = effect_match.group(1).strip()

    # 발명을 실시하기 위한 구체적인 내용 추출 (개선된 패턴)
    detailed_pattern = r"(?:【발명을 실시하기 위한 구체적인 내용】|발명을\s*실시하기\s*위한\s*구체적인\s*내용)\s*(.*?)(?:【청구범위】|청구\s*범위|【요약】)"
    detailed_match = re.search(detailed_pattern, ocr_text, re.DOTALL | re.IGNORECASE)
    if detailed_match:
        patent_draft["patent_draft_detailed"] = detailed_match.group(1).strip()
    
    # 요약 추출 (개선된 패턴)
    summary_pattern = r"(?:【요약】|요\s*약)\s*(.*?)(?:$)"
    summary_match = re.search(summary_pattern, ocr_text, re.DOTALL | re.IGNORECASE)
    if summary_match:
        patent_draft["patent_draft_summary"] = summary_match.group(1).strip()
    
    # 청구항을 번호별로 저장할 딕셔너리
    claims_dict = {}
    
    # 패턴 1: 청구항 번호가 명시적으로 표시된 경우 (청구항 1, 청구항 2)
    explicit_pattern = r"(?:【청구항\s*(\d+)】|청구항\s*(\d+))\s*(.*?)(?=(?:【청구항\s*\d+】|청구항\s*\d+|제\s*\d+\s*항|【요약】|요\s*약|$))"
    for match in re.finditer(explicit_pattern, ocr_text, re.DOTALL | re.IGNORECASE):
        claim_num = match.group(1) or match.group(2)
        claim_text = match.group(3).strip()
        if claim_text and "요약" not in claim_text[:10].lower():
            claims_dict[claim_num] = f"【청구항 {claim_num}】 {claim_text}"
    
    # 패턴 2: 종속 청구항 (제 N 항에 있어서)
    dependent_pattern = r"제\s*(\d+)\s*항에?\s*있어서[,]?\s*(.*?)(?=(?:【청구항\s*\d+】|청구항\s*\d+|제\s*\d+\s*항|【요약】|요\s*약|$))"
    for match in re.finditer(dependent_pattern, ocr_text, re.DOTALL | re.IGNORECASE):
        ref_claim = match.group(1)
        claim_text = match.group(2).strip()
        
        # 가장 간단한 방법: 종속 청구항 번호는 참조 청구항 번호 + 1로 가정
        # 실제로는 여러 종속 청구항이 같은 청구항을 참조할 수 있음
        dependent_num = str(int(ref_claim) + 1)
        
        if claim_text and "요약" not in claim_text[:10].lower():
            # 기존 청구항 내용이 비어있거나 없는 경우에만 추가
            if dependent_num not in claims_dict or not claims_dict[dependent_num].strip():
                claims_dict[dependent_num] = f"【청구항 {dependent_num}】 제 {ref_claim} 항에 있어서, {claim_text}"
    
    # 패턴 3: 청구항 헤더만 있고 내용이 분리된 경우
    if "2" in claims_dict and not claims_dict["2"].strip().replace(f"【청구항 2】", "").strip():
        orphan_text = re.search(r"【청구항\s*2】.*?(?:\n\n)(.*?)(?=(?:【청구항\s*\d+】|청구항\s*\d+|제\s*\d+\s*항|【요약】|요\s*약|$))", ocr_text, re.DOTALL | re.IGNORECASE)
        if orphan_text and orphan_text.group(1).strip():
            claims_dict["2"] = f"【청구항 2】 {orphan_text.group(1).strip()}"
    
    # 청구항이 비어있는 경우를 제거
    claims_dict = {k: v for k, v in claims_dict.items() if v.replace(f"【청구항 {k}】", "").strip()}
    
    if claims_dict:
        # 청구항 번호로 정렬
        sorted_claims = [claims_dict[str(i)] for i in sorted(map(int, claims_dict.keys()))]
        patent_draft["patent_draft_claim"] = "\n\n".join(sorted_claims)
    
    return patent_draft


async def extract_text(pdf_path: str) -> str:
    """PDF에서 텍스트 추출 (Google Cloud Vision API 사용)"""
    try:
        # Vision API 클라이언트 초기화
        client = vision.ImageAnnotatorClient()
        
        # Poppler 경로 지정 (Windows 환경)
        poppler_path = r"C:\SSAFY\poppler-24.08.0\Library\bin"
        
        # PDF를 이미지로 변환
        pages = convert_from_path(pdf_path, 300, poppler_path=poppler_path)
        text_result = ""
        
        logger.info(f"PDF 파일({pdf_path})을 {len(pages)}페이지의 이미지로 변환 완료")
        
        # 각 페이지를 Vision API로 처리
        for i, page in enumerate(pages):
            logger.info(f"페이지 {i+1}/{len(pages)} OCR 처리 중...")
            
            # 이미지를 메모리에 저장
            img_byte_arr = io.BytesIO()
            page.save(img_byte_arr, format='PNG')
            content = img_byte_arr.getvalue()
            
            # Vision API 요청
            image = vision.Image(content=content)
            
            # 문서 텍스트 감지 요청 (한국어+영어 힌트 포함)
            response = client.document_text_detection(
                image=image,
                image_context={"language_hints": ["ko", "en"]}
            )
            
            # 페이지별 결과 저장
            text = response.full_text_annotation.text
            text_result += f"\n--- 페이지 {i+1} ---\n{text}"
            
            logger.info(f"페이지 {i+1} OCR 완료: {len(text)} 글자 추출")
            
        return text_result
    except Exception as e:
        logger.error(f"Vision API 처리 중 오류: {str(e)}")
        raise Exception(f"OCR 처리 중 오류: {str(e)}")

@router.post("/pdf/parse-patent", response_model=Dict[str, Any])
async def parse_patent_from_pdf(file: UploadFile = File(...)):
    """특허 PDF를 업로드하고 분석 결과를 JSON으로 반환"""
    # 임시 파일로 저장
    temp_file_path = f"{TEMP_DIR}/{file.filename}"
    
    with open(temp_file_path, "wb") as f:
        content = await file.read()
        f.write(content)
    
    try:
        # Google Cloud Vision API로 텍스트 추출
        logger.info(f"PDF 파일 OCR 처리 시작: {temp_file_path}")
        ocr_text = extract_text(temp_file_path)
        logger.info(f"PDF 파일 OCR 처리 완료: {len(ocr_text)} 글자 추출")
        
        # 특허 섹션 파싱
        logger.info("특허 문서 섹션 파싱 시작")
        patent_data = parse_patent_document(ocr_text)
        logger.info("특허 문서 섹션 파싱 완료")
        
        # 임시 파일 삭제
        if os.path.exists(temp_file_path):
            os.remove(temp_file_path)
            logger.info(f"임시 파일 삭제 완료: {temp_file_path}")
        
        # 기본 제목 설정 (파일명이나 추출된 제목)
        if not patent_data["patent_draft_title"]:
            patent_data["patent_draft_title"] = file.filename.replace(".pdf", "")
        
        return {
            "data": patent_data
        }
        
    except Exception as e:
        # 에러 발생 시 임시 파일 정리
        if os.path.exists(temp_file_path):
            os.remove(temp_file_path)
            
        logger.error(f"특허 PDF 파싱 중 오류: {str(e)}")
        raise HTTPException(status_code=500, detail=f"특허 PDF 파싱 중 오류: {str(e)}")

