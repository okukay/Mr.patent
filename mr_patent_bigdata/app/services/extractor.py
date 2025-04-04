import re
from typing import List, Dict, Any
from striprtf.striprtf import rtf_to_text
from app.core.logging import logger

def extract_patent_data(rtf_text):
    """RTF 텍스트에서 특허 데이터 추출 - 구분선 기반 접근법"""
    logger.info(f"RTF 텍스트 길이: {len(rtf_text)} 문자")
    
    # 구분선 패턴 (적어도 10개 이상의 하이픈으로 구성)
    separator = r'-{10,}'
    
    # 전체 파일을 구분선으로 분할
    chunks = re.split(separator, rtf_text)
    logger.info(f"구분선으로 분할된 총 청크 수: {len(chunks)}")
    
    # 특허 데이터를 담을 리스트
    patents = []
    current_patent = {}
    
    # 특허 번호 패턴
    application_pattern = r'출원번호\s*:\s*(\S+)'
    
    # 각 청크 처리
    for i, chunk in enumerate(chunks):
        # 출원번호가 있는 청크 찾기
        app_match = re.search(application_pattern, chunk)
        if app_match:
            # 새 특허 시작 (이전 특허가 있으면 저장)
            if current_patent and 'application_number' in current_patent:
                if all(current_patent.get(k) for k in ["application_number", "title"]):
                    # 필수 필드가 없으면 기본값 설정
                    if 'summary' not in current_patent:
                        current_patent['summary'] = "내용 없음"
                    if 'claims' not in current_patent:
                        current_patent['claims'] = "내용 없음"
                    if 'ipc_classification' not in current_patent:
                        current_patent['ipc_classification'] = "분류 없음"
                    
                    patents.append(current_patent)
            
            # 새 특허 정보 시작
            current_patent = {'application_number': app_match.group(1).strip()}
            
            # 발명 제목 추출
            title_match = re.search(r'발명의명칭\s*:\s*(.+?)(?=\(|$)', chunk, re.DOTALL)
            if title_match:
                current_patent['title'] = title_match.group(1).strip()
            
            # 요약 추출
            summary_match = re.search(r'\(요약\)\s*(.+?)(?=\(청구항\)|$)', chunk, re.DOTALL)
            if summary_match:
                current_patent['summary'] = summary_match.group(1).strip()
            
            # 청구항 추출
            claims_match = re.search(r'\(청구항\)\s*(.+?)(?=IPC분류|$)', chunk, re.DOTALL)
            if claims_match:
                current_patent['claims'] = claims_match.group(1).strip()
            
            # IPC 분류 추출
            ipc_match = re.search(r'IPC분류\s*:\s*(.+)', chunk)
            if ipc_match:
                current_patent['ipc_classification'] = ipc_match.group(1).strip()
        
        # 마지막 특허 또는 IPC 정보가 다음 청크에 있는 경우 처리
        elif current_patent and 'application_number' in current_patent:
            ipc_match = re.search(r'IPC분류\s*:\s*(.+)', chunk)
            if ipc_match and 'ipc_classification' not in current_patent:
                current_patent['ipc_classification'] = ipc_match.group(1).strip()
    
    # 마지막 특허 추가
    if current_patent and 'application_number' in current_patent:
        if all(current_patent.get(k) for k in ["application_number", "title"]):
            # 필수 필드가 없으면 기본값 설정
            if 'summary' not in current_patent:
                current_patent['summary'] = "내용 없음"
            if 'claims' not in current_patent:
                current_patent['claims'] = "내용 없음"
            if 'ipc_classification' not in current_patent:
                current_patent['ipc_classification'] = "분류 없음"
                
            patents.append(current_patent)
    
    # 파일명에서 IPC 코드 추출을 위한 함수
    def extract_ipc_from_filename(filename):
        match = re.match(r'([a-zA-Z]\d+)_\d+_\d+\.rtf', filename)
        return match.group(1) if match else None
    
    logger.info(f"추출된 특허 수: {len(patents)}")
    return patents

def prepare_corpus(patents: List[Dict[str, Any]]) -> List[str]:
    """특허 데이터로부터 코퍼스 준비"""
    corpus = []
    for patent in patents:
        title = patent.get("title", "")
        summary = patent.get("summary", "")
        claims = patent.get("claims", "")
        combined_text = f"{title} {summary} {claims}"
        corpus.append(combined_text)
    return corpus
