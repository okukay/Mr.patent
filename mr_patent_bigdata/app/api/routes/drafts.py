from fastapi import APIRouter, HTTPException, Depends, Query
from typing import List, Optional, Dict, Any
from datetime import datetime, timezone
import logging
from fastapi.responses import FileResponse
from fpdf import FPDF  # reportlab 대신 fpdf2 사용
import tempfile
import os
import re

from app.core.database import database
from app.schemas.patent import PatentDraftCreate, PatentDraftResponse
from app.services.vectorizer import get_tfidf_vector, get_bert_vector
import numpy as np

router = APIRouter(prefix="/fastapi", tags=["drafts"])

# 로깅 설정
logger = logging.getLogger(__name__)

def get_current_timestamp():
    """현재 시간을 ISO 8601 형식으로 변환 (UTC)"""
    return datetime.now(timezone.utc).isoformat().replace('+00:00', 'Z')

@router.get("/draft/{patent_draft_id}", response_model=Dict[str, Any])
async def get_patent_draft(patent_draft_id: int):
    """특정 ID의 특허 초안 조회"""
    query = """
    SELECT * FROM patent_draft
    WHERE patent_draft_id = :draft_id
    """
    
    draft = await database.fetch_one(
        query=query,
        values={"draft_id": patent_draft_id}
    )
    
    if not draft:
        raise HTTPException(
            status_code=404,
            detail={
                "code": "DRAFT_NOT_FOUND",
                "message": "해당 ID의 특허 초안을 찾을 수 없습니다.",
                "timestamp": get_current_timestamp()
            }
        )
    
    # 바이너리 벡터 필드 제외
    draft_dict = {k: v for k, v in dict(draft).items() if not k.endswith('_vector')}
    
    # 날짜/시간 필드 변환
    if "patent_draft_created_at" in draft_dict:
        draft_dict["created_at"] = draft_dict.pop("patent_draft_created_at").isoformat() + 'Z'
    if "patent_draft_updated_at" in draft_dict:
        draft_dict["updated_at"] = draft_dict.pop("patent_draft_updated_at").isoformat() + 'Z'
    
    return {
        "data": draft_dict,
        "timestamp": get_current_timestamp()
    }

@router.post("/folder/{user_patent_folder_id}/draft", response_model=Dict[str, Any])
async def create_or_update_draft(
    user_patent_folder_id: int,
    draft: PatentDraftCreate
):
    """특허 초안 저장 또는 수정"""
    try:
        # 폴더 존재 확인
        folder_query = """
        SELECT * FROM user_patent_folder 
        WHERE user_patent_folder_id = :folder_id
        """
        folder = await database.fetch_one(
            query=folder_query, 
            values={"folder_id": user_patent_folder_id}
        )
        
        if not folder:
            raise HTTPException(
                status_code=404,
                detail={
                    "code": "FOLDER_NOT_FOUND",
                    "message": "지정한 폴더를 찾을 수 없습니다.",
                    "timestamp": get_current_timestamp()
                }
            )
        
        now = datetime.now(timezone.utc)
        
        # 각 필드에 대한 벡터 생성
        vector_fields = {}
        fields = ["title", "technical_field", "background", "problem", 
                "solution", "effect", "detailed", "summary", "claim"]
        
        for field in fields:
            text = getattr(draft, f"patent_draft_{field}", "")
            if text:
                try:
                    # 벡터 생성
                    tfidf_vector = get_tfidf_vector(text)
                    bert_vector = get_bert_vector(text)
                    
                    # 벡터 필드에 저장
                    vector_fields[f"patent_draft_{field}_tfidf_vector"] = tfidf_vector.tobytes()
                    vector_fields[f"patent_draft_{field}_bert_vector"] = bert_vector.tobytes()
                except Exception as e:
                    # 벡터화 실패 시 로그 기록
                    logger.error(f"벡터화 실패 ({field}): {str(e)}")
                    # 빈 벡터 할당
                    vector_fields[f"patent_draft_{field}_tfidf_vector"] = np.zeros(1000).tobytes()
                    vector_fields[f"patent_draft_{field}_bert_vector"] = np.zeros(768).tobytes()
        
        # 초안이 이미 존재하는지 확인
        existing_draft_query = """
        SELECT * FROM patent_draft
        WHERE user_patent_folder_id = :folder_id
        AND patent_draft_id = :draft_id
        """
        
        draft_id = getattr(draft, "patent_draft_id", None)
        existing_draft = None
        
        if draft_id:
            existing_draft = await database.fetch_one(
                query=existing_draft_query,
                values={
                    "folder_id": user_patent_folder_id,
                    "draft_id": draft_id
                }
            )
        
        if existing_draft:
            # 초안 업데이트
            update_fields = ", ".join([
                f"patent_draft_{field} = :{field}" 
                for field in ["title", "technical_field", "background", "problem", 
                            "solution", "effect", "detailed", "summary", "claim"]
            ])
            update_fields += ", patent_draft_updated_at = :updated_at"
            
            # 벡터 필드 추가
            for field_name in vector_fields.keys():
                field_suffix = field_name.split('patent_draft_')[1]
                update_fields += f", {field_name} = :{field_suffix}"
            
            update_query = f"""
            UPDATE patent_draft
            SET {update_fields}
            WHERE patent_draft_id = :draft_id
            """
            
            # 안전하게 draft 필드만 추출
            draft_values = {}
            for field in draft.__dict__:
                if field.startswith("patent_draft_"):
                    field_suffix = field.split("patent_draft_")[1]
                    draft_values[field_suffix] = getattr(draft, field)
            
            values = draft_values
            values.update({
                "updated_at": now,
                "draft_id": existing_draft["patent_draft_id"]
            })
            
            # 벡터 값 추가 (안전하게)
            for field_name, vector_value in vector_fields.items():
                field_suffix = field_name.split('patent_draft_')[1]
                values[field_suffix] = vector_value
            
            # id 키가 포함되어 있으면 제거 (SQL 바인딩 오류 방지)
            if 'id' in values and ':id' not in update_query:
                values.pop('id')
            
            await database.execute(query=update_query, values=values)
            
            return {
                "data": {
                    "patent_draft_id": existing_draft["patent_draft_id"],
                    "created_at": now.isoformat().replace('+00:00', 'Z')
                },
                "timestamp": get_current_timestamp()
            }
        else:
            # 새 초안 생성
            field_names = ["user_patent_folder_id"]
            field_names.extend([f"patent_draft_{field}" for field in [
                "title", "technical_field", "background", "problem", 
                "solution", "effect", "detailed", "summary", "claim"
            ]])
            field_names.extend(["patent_draft_created_at", "patent_draft_updated_at"])
            # 벡터 필드 추가
            field_names.extend(vector_fields.keys())
            
            # 안전한 바인딩 파라미터 생성
            placeholders = []
            for field in field_names:
                if field == "user_patent_folder_id":
                    placeholders.append(":user_patent_folder_id")
                elif field == "patent_draft_created_at":
                    placeholders.append(":created_at")
                elif field == "patent_draft_updated_at":
                    placeholders.append(":updated_at")
                else:
                    field_suffix = field.split('patent_draft_')[1]
                    placeholders.append(f":{field_suffix}")
            
            insert_query = f"""
            INSERT INTO patent_draft (
                {', '.join(field_names)}
            ) VALUES (
                {', '.join(placeholders)}
            )
            """
            
            # 안전하게 draft 필드만 추출
            draft_values = {}
            for field in draft.__dict__:
                if field.startswith("patent_draft_"):
                    field_suffix = field.split("patent_draft_")[1]
                    draft_values[field_suffix] = getattr(draft, field)
            
            values = draft_values
            values.update({
                "user_patent_folder_id": user_patent_folder_id,
                "created_at": now,
                "updated_at": now
            })
            
            # 벡터 값 추가 (안전하게)
            for field_name, vector_value in vector_fields.items():
                field_suffix = field_name.split('patent_draft_')[1]
                values[field_suffix] = vector_value
            
            # id 키가 포함되어 있으면 제거 (SQL 바인딩 오류 방지)
            if 'id' in values and ':id' not in insert_query:
                values.pop('id')
                
            draft_id = await database.execute(query=insert_query, values=values)
            
            return {
                "data": {
                    "patent_draft_id": draft_id,
                    "created_at": now.isoformat().replace('+00:00', 'Z')
                },
                "timestamp": get_current_timestamp()
            }
    except Exception as e:
        logger.error(f"특허 초안 저장 중 오류: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail={
                "code": "INTERNAL_ERROR",
                "message": f"특허 초안 저장 중 오류가 발생했습니다: {str(e)}",
                "timestamp": get_current_timestamp()
            }
        )

@router.get("/drafts/recent", response_model=Dict[str, Any])
async def get_recent_drafts(
    user_id: int, 
    limit: int = Query(5, ge=1, le=50, description="조회할 최대 항목 수")
):
    """사용자의 최근 특허 초안 목록 조회 (최대 5개) - 벡터 필드 제외한 모든 정보 반환"""
    try:
        # 사용자 폴더 확인
        folder_query = """
        SELECT user_patent_folder_id FROM user_patent_folder 
        WHERE user_id = :user_id
        """
        
        folders = await database.fetch_all(
            query=folder_query,
            values={"user_id": user_id}
        )
        
        if not folders:
            return {
                "data": {"patent_drafts": []},
                "timestamp": get_current_timestamp()
            }
        
        # 폴더 ID 목록 추출
        folder_ids = [folder["user_patent_folder_id"] for folder in folders]
        folder_ids_str = ",".join(map(str, folder_ids))
        
        # 최근 특허 초안 조회 - 벡터 필드를 제외한 모든 정보 가져오기
        draft_query = f"""
        SELECT 
            patent_draft_id,
            user_patent_folder_id,
            patent_draft_title,
            patent_draft_technical_field,
            patent_draft_background,
            patent_draft_problem,
            patent_draft_solution,
            patent_draft_effect,
            patent_draft_detailed,
            patent_draft_summary,
            patent_draft_claim,
            patent_draft_created_at,
            patent_draft_updated_at
        FROM patent_draft
        WHERE user_patent_folder_id IN ({folder_ids_str})
        ORDER BY patent_draft_updated_at DESC
        LIMIT :limit
        """
        
        drafts = await database.fetch_all(
            query=draft_query,
            values={"limit": limit}
        )
        
        # 결과 포맷팅
        result_drafts = []
        for draft in drafts:
            draft_dict = dict(draft)
            
            # 날짜 포맷 변환
            if "patent_draft_created_at" in draft_dict:
                draft_dict["created_at"] = draft_dict.pop("patent_draft_created_at").isoformat() + 'Z'
            if "patent_draft_updated_at" in draft_dict:
                draft_dict["updated_at"] = draft_dict.pop("patent_draft_updated_at").isoformat() + 'Z'
            
            # 필드명 정리 (선택 사항)
            formatted_draft = {}
            for key, value in draft_dict.items():
                if key.startswith("patent_draft_"):
                    # patent_draft_ 접두사 제거
                    formatted_key = key[len("patent_draft_"):]
                    formatted_draft[formatted_key] = value
                else:
                    formatted_draft[key] = value
            
            result_drafts.append(formatted_draft)
        
        return {
            "data": {"patent_drafts": result_drafts},
            "timestamp": get_current_timestamp()
        }
        
    except Exception as e:
        logger.error(f"최근 특허 초안 조회 중 오류: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail={
                "message": f"서버 오류가 발생했습니다: {str(e)}",
                "timestamp": get_current_timestamp()
            }
        )

@router.get("/draft/{patent_draft_id}/export-pdf", response_class=FileResponse)
async def export_patent_draft_pdf(patent_draft_id: int):
    """특허 초안 PDF 다운로드 (FPDF 사용)"""
    # 초안 정보 조회
    draft_query = """
    SELECT * FROM patent_draft
    WHERE patent_draft_id = :draft_id
    """
    
    draft = await database.fetch_one(
        query=draft_query,
        values={"draft_id": patent_draft_id}
    )
    
    if not draft:
        raise HTTPException(
            status_code=404,
            detail={
                "code": "DRAFT_NOT_FOUND",
                "message": "해당 특허 초안을 찾을 수 없습니다.",
                "timestamp": get_current_timestamp()
            }
        )
    
    # 임시 PDF 파일 생성
    temp_file = tempfile.NamedTemporaryFile(delete=False, suffix=".pdf")
    pdf_path = temp_file.name
    temp_file.close()
    
    try:
        # FPDF 객체 생성
        pdf = FPDF()
        pdf.add_page()
        
        # 다양한 경로에서 한글 폰트 찾기 (Windows/Linux 호환)
        font_found = False
        possible_font_paths = [
            # 1. 프로젝트 내부 경로 (상대 경로)
            os.path.join(os.path.dirname(__file__), '..', 'static', 'fonts', 'NanumGothic.ttf'),
            # 2. 프로젝트 루트 기준 경로
            os.path.join(os.getcwd(), 'app', 'static', 'fonts', 'NanumGothic.ttf'),
            # 3. Windows 시스템 폰트
            os.path.join('C:', os.sep, 'Windows', 'Fonts', 'malgun.ttf'),
            # 4. Linux 시스템 폰트 (EC2 배포 시)
            '/usr/share/fonts/truetype/nanum/NanumGothic.ttf',
            '/usr/share/fonts/truetype/nanum/NanumGothicBold.ttf',
            '/usr/share/fonts/truetype/nanum/NanumGothic_Coding.ttf'
        ]
        
        # 폰트 파일 존재 확인 및 등록
        for font_path in possible_font_paths:
            if os.path.exists(font_path):
                try:
                    # 한글 폰트 등록 (유니코드 지원 활성화)
                    pdf.add_font('CustomFont', '', font_path, uni=True)
                    pdf.set_font('CustomFont', '', 16)
                    font_found = True
                    logger.info(f"한글 폰트 로드 성공: {font_path}")
                    break
                except Exception as font_error:
                    logger.warning(f"폰트 로드 시도 중 오류 ({font_path}): {str(font_error)}")
        
        # 모든 폰트 로드 시도 실패 시
        if not font_found:
            # PDF 생성은 계속하되 한글은 표시되지 않을 것임을 경고
            pdf.set_font('Arial', '', 16)
            logger.warning("한글 폰트를 찾을 수 없습니다. 한글이 제대로 표시되지 않을 수 있습니다.")
        
        # 특수 문자 제거 함수
        def clean_text(text):
            if not text:
                return ""
            # 특수 구분자 제거
            return text.replace("■■■", "").replace("■", "")
        
        # 제목
        pdf.cell(0, 10, draft["patent_draft_title"], 0, 1, 'C')
        pdf.ln(5)
        
        # 각 섹션 출력
        sections = [
            ("기술분야", draft["patent_draft_technical_field"]),
            ("배경기술", draft["patent_draft_background"]),
            ("해결하려는 과제", draft["patent_draft_problem"]),
            ("과제의 해결 수단", draft["patent_draft_solution"]),
            ("발명의 효과", draft["patent_draft_effect"]),
            ("발명을 실시하기 위한 구체적인 내용", draft["patent_draft_detailed"]),
            ("요약", draft["patent_draft_summary"]),
            ("청구항", draft["patent_draft_claim"])
        ]
        
        for title, content in sections:
            if not content:
                continue
            
            # 섹션 제목
            if font_found:
                pdf.set_font('CustomFont', '', 12)
            else:
                pdf.set_font('Arial', 'B', 12)
            pdf.cell(0, 10, title, 0, 1)
            
            # 섹션 내용
            if content:
                if font_found:
                    pdf.set_font('CustomFont', '', 10)
                else:
                    pdf.set_font('Arial', '', 10)
                # 내용 정리
                clean_content = clean_text(content)
                # 단락별로 출력
                for paragraph in clean_content.split('\n'):
                    if paragraph.strip():
                        pdf.multi_cell(0, 6, paragraph)
                pdf.ln(3)
            
            pdf.ln(5)
        
        # PDF 저장
        pdf.output(pdf_path)
        
        # 파일 응답
        response = FileResponse(
            path=pdf_path,
            filename=f"patent_draft_{patent_draft_id}.pdf",
            media_type="application/pdf"
        )
        
        # 임시 파일 삭제 설정
        response.headers["X-Delete-After-Sent"] = "true"
        
        return response
        
    except Exception as e:
        logger.error(f"PDF 생성 중 오류: {str(e)}")
        
        if os.path.exists(pdf_path):
            try:
                os.unlink(pdf_path)
            except:
                pass
                
        raise HTTPException(
            status_code=500,
            detail={
                "code": "PDF_GENERATION_ERROR",
                "message": f"PDF 생성 중 오류가 발생했습니다: {str(e)}",
                "timestamp": get_current_timestamp()
            }
        )

@router.patch("/folder/{folder_id}", response_model=Dict[str, Any])
async def update_folder_name(
    folder_id: int,
    folder_data: dict
):
    """특허 폴더명 수정"""
    try:
        if "user_patent_folder_title" not in folder_data:
            raise HTTPException(
                status_code=400,
                detail={
                    "code": "INVALID_REQUEST",
                    "message": "폴더명(user_patent_folder_title)은 필수 항목입니다.",
                    "timestamp": get_current_timestamp()
                }
            )
            
        # 폴더 존재 확인
        folder_query = """
        SELECT * FROM user_patent_folder 
        WHERE user_patent_folder_id = :folder_id
        """
        
        folder = await database.fetch_one(
            query=folder_query,
            values={"folder_id": folder_id}
        )
        
        if not folder:
            raise HTTPException(
                status_code=404,
                detail={
                    "code": "FOLDER_NOT_FOUND",
                    "message": "지정한 폴더를 찾을 수 없습니다.",
                    "timestamp": get_current_timestamp()
                }
            )
        
        # 폴더명 업데이트
        update_query = """
        UPDATE user_patent_folder
        SET user_patent_folder_title = :title,
            user_patent_folder_updated_at = :updated_at
        WHERE user_patent_folder_id = :folder_id
        """
        
        now = datetime.now(timezone.utc)
        await database.execute(
            query=update_query,
            values={
                "title": folder_data["user_patent_folder_title"],
                "updated_at": now,
                "folder_id": folder_id
            }
        )
        
        # 업데이트된 폴더 정보 조회
        updated_folder = await database.fetch_one(
            query=folder_query,
            values={"folder_id": folder_id}
        )
        
        # 결과 포맷팅
        result = dict(updated_folder)
        result["created_at"] = result.pop("user_patent_folder_created_at").isoformat() + 'Z'
        result["updated_at"] = result.pop("user_patent_folder_updated_at").isoformat() + 'Z'
        
        return {
            "data": result,
            "timestamp": get_current_timestamp()
        }
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"폴더명 수정 중 오류: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail={
                "code": "INTERNAL_ERROR",
                "message": f"폴더명 수정 중 오류가 발생했습니다: {str(e)}",
                "timestamp": get_current_timestamp()
            }
        )

@router.get("/folder/{user_patent_folder_id}/reports", response_model=Dict[str, Any])
async def get_folder_reports(user_patent_folder_id: int):
    """폴더별 유사도 분석 리포트 목록 조회"""
    # 폴더의 유사도 분석 결과 목록 조회
    query = """
    SELECT s.similarity_id, pd.patent_draft_id, s.similarity_created_at,
           COUNT(sp.similarity_patent_id) as similar_patents_count
    FROM similarity s
    JOIN patent_draft pd ON s.patent_draft_id = pd.patent_draft_id
    LEFT JOIN similarity_patent sp ON s.similarity_id = sp.similarity_id
    WHERE pd.user_patent_folder_id = :folder_id
    GROUP BY s.similarity_id
    ORDER BY s.similarity_created_at DESC
    """
    
    reports = await database.fetch_all(
        query=query,
        values={"folder_id": user_patent_folder_id}
    )
    
    if not reports:
        return {
            "data": {"reports": []},
            "timestamp": get_current_timestamp()
        }
    
    # 결과 포맷팅
    result_reports = []
    for report in reports:
        result_reports.append({
            "similarity_id": report["similarity_id"],
            "patent_draft_id": report["patent_draft_id"],
            "created_at": report["similarity_created_at"].isoformat() + 'Z',
            "similar_patents_count": report["similar_patents_count"]
        })
    
    return {
        "data": {
            "reports": result_reports
        },
        "timestamp": get_current_timestamp()
    }
