import os
import json
from datetime import datetime
from typing import Dict, Any
from fpdf import FPDF

# 리포트 저장 디렉토리
REPORTS_DIR = "reports"
os.makedirs(REPORTS_DIR, exist_ok=True)

# 텍스트 인코딩 이슈 해결을 위한 유틸리티 함수
def sanitize_text(text):
    """latin-1 인코딩 가능한 문자로 변환"""
    if not text:
        return ""
    # 한글이나 특수문자는 '?' 또는 빈 문자로 대체
    return ''.join(c if ord(c) < 256 else '?' for c in str(text))

async def generate_report_pdf(report_data: Dict[str, Any]) -> str:
    """특허 분석 결과를 PDF로 생성"""
    folder = report_data.get("folder", {})
    draft = report_data.get("draft", {})
    similar_patents = report_data.get("similar_patents", [])
    fitness = report_data.get("fitness", {})
    comparisons = report_data.get("comparisons", [])
    
    # PDF 생성
    pdf = FPDF()
    pdf.add_page()
    
    # 기본 폰트 사용 (한글 지원 불가)
    pdf.set_font("Arial", "", 12)
    
    # 제목 설정
    pdf.set_font("Arial", "B", 16)
    pdf.cell(0, 10, "Patent Similarity Analysis Report", 0, 1, "C")
    pdf.ln(10)
    
    # 기본 정보
    pdf.set_font("Arial", "B", 12)
    pdf.cell(0, 10, "1. Patent Draft Information", 0, 1)
    
    pdf.set_font("Arial", "", 11)
    pdf.cell(0, 8, f"Title: {sanitize_text(draft.get('patent_draft_title', ''))}", 0, 1)
    pdf.cell(0, 8, f"Folder: {sanitize_text(folder.get('user_patent_folder_title', ''))}", 0, 1)
    pdf.cell(0, 8, f"Created: {sanitize_text(draft.get('patent_draft_created_at', ''))}", 0, 1)
    pdf.ln(5)
    
    # 적합도 결과
    pdf.set_font("Arial", "B", 12)
    pdf.cell(0, 10, "2. Fitness Test Results", 0, 1)
    
    pdf.set_font("Arial", "", 11)
    fitness_result = "Pass" if fitness.get("fitness_is_corrected") else "Fail"
    pdf.cell(0, 8, f"Fitness Result: {fitness_result}", 0, 1)
    
    # fitness_details가 문자열인 경우 JSON으로 파싱
    fitness_details = fitness.get("fitness_good_content", {})
    if isinstance(fitness_details, str):
        try:
            fitness_details = json.loads(fitness_details)
        except (json.JSONDecodeError, TypeError):
            fitness_details = {}
    
    # 딕셔너리인지 확인 후 항목 출력
    if isinstance(fitness_details, dict):
        for field, is_valid in fitness_details.items():
            field_name = sanitize_text(field)
            if isinstance(is_valid, bool):
                result = "Valid" if is_valid else "Invalid"
            else:
                result = sanitize_text(str(is_valid))
            pdf.cell(0, 8, f"{field_name}: {result}", 0, 1)
    pdf.ln(5)
    
    # 유사 특허 목록
    pdf.set_font("Arial", "B", 12)
    pdf.cell(0, 10, "3. Similar Patents", 0, 1)
    
    pdf.set_font("Arial", "", 11)
    for i, patent in enumerate(similar_patents[:3], 1):
        pdf.cell(0, 8, f"{i}. {sanitize_text(patent.get('patent_title', ''))}", 0, 1)
        pdf.cell(0, 8, f"   Application #: {sanitize_text(patent.get('patent_application_number', ''))}", 0, 1)
        pdf.cell(0, 8, f"   Similarity: {float(patent.get('similarity_patent_score', 0)) * 100:.2f}%", 0, 1)
        pdf.ln(3)
    pdf.ln(5)
    
    # 상세 비교 결과
    pdf.set_font("Arial", "B", 12)
    pdf.cell(0, 10, "4. Detailed Comparison", 0, 1)
    
    pdf.set_font("Arial", "", 11)
    for i, comp in enumerate(comparisons[:3], 1):
        pdf.cell(0, 8, f"Comparison {i}:", 0, 1)
        pdf.cell(0, 8, f"Total Score: {float(comp.get('detailed_comparison_total_score', 0)) * 100:.2f}%", 0, 1)
        
        # 컨텍스트 데이터가 있는 경우 표시
        context_data = comp.get("detailed_comparison_context", {})
        # 컨텍스트 데이터가 문자열인 경우 JSON으로 파싱
        if isinstance(context_data, str):
            try:
                context_data = json.loads(context_data)
            except (json.JSONDecodeError, TypeError):
                context_data = {}
        
        if isinstance(context_data, dict) and "highlights" in context_data:
            for j, highlight in enumerate(context_data["highlights"][:3], 1):
                pdf.cell(0, 8, f"Similar Section {j}:", 0, 1)
                pdf.multi_cell(0, 8, f"Draft: {sanitize_text(highlight.get('original', ''))}")
                pdf.multi_cell(0, 8, f"Patent: {sanitize_text(highlight.get('similar', ''))}")
                pdf.cell(0, 8, f"Similarity: {float(highlight.get('similarity', 0)) * 100:.2f}%", 0, 1)
                pdf.ln(2)
        pdf.ln(3)
    
    # 파일 생성 날짜로 파일명 생성
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    
    # draft_id로 파일명 변경 (폴더 ID 대신)
    draft_id = draft.get("patent_draft_id", "")
    filename = f"patent_report_draft_{draft_id}_{timestamp}.pdf"
    file_path = os.path.join(REPORTS_DIR, filename)
    
    try:
        # PDF 저장
        pdf.output(file_path)
        return file_path
    except Exception as e:
        # 오류 발생 시 빈 PDF 생성
        emergency_pdf = FPDF()
        emergency_pdf.add_page()
        emergency_pdf.set_font("Arial", "B", 16)
        emergency_pdf.cell(0, 10, "Error creating full report", 0, 1, "C")
        emergency_pdf.set_font("Arial", "", 12)
        emergency_pdf.cell(0, 10, f"Error: {str(e)}", 0, 1)
        emergency_pdf.output(file_path)
        return file_path
