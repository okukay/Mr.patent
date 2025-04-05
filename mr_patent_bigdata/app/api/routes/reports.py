from fastapi import APIRouter, HTTPException
from typing import Dict, Any, List
from datetime import datetime, timezone
import os
from fastapi.responses import FileResponse

from app.core.database import database
from app.services.report_generator import generate_report_pdf

router = APIRouter(prefix="/fastapi", tags=["reports"])

def get_current_timestamp():
    """현재 시간을 ISO 8601 형식으로 변환 (UTC)"""
    return datetime.now(timezone.utc).isoformat().replace('+00:00', 'Z')

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

@router.get("/patent/{patent_draft_id}/report")
async def download_draft_report(patent_draft_id: int):
    """특허 초안 분석 리포트 다운로드"""
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
    
    # 초안 ID를 사용하여 각종 데이터 조회
    draft_dict = dict(draft)
    
    # 폴더 정보 조회
    folder_query = """
    SELECT * FROM user_patent_folder
    WHERE user_patent_folder_id = :folder_id
    """
    
    folder = await database.fetch_one(
        query=folder_query,
        values={"folder_id": draft_dict["user_patent_folder_id"]}
    )
    
    # 유사도 분석 결과 조회
    similarity_query = """
    SELECT * FROM similarity
    WHERE patent_draft_id = :draft_id
    ORDER BY similarity_created_at DESC
    LIMIT 1
    """
    
    similarity = await database.fetch_one(
        query=similarity_query,
        values={"draft_id": patent_draft_id}
    )
    
    if not similarity:
        raise HTTPException(
            status_code=404,
            detail={
                "code": "SIMILARITY_NOT_FOUND",
                "message": "유사도 분석 결과를 찾을 수 없습니다.",
                "timestamp": get_current_timestamp()
            }
        )
    
    # 유사 특허 결과 조회
    similar_patents_query = """
    SELECT sp.*, p.patent_title, p.patent_application_number
    FROM similarity_patent sp
    JOIN patent p ON sp.patent_id = p.patent_id
    WHERE sp.similarity_id = :similarity_id
    ORDER BY sp.similarity_patent_score DESC
    """
    
    similar_patents = await database.fetch_all(
        query=similar_patents_query,
        values={"similarity_id": similarity["similarity_id"]}
    )
    
    # 상세 비교 결과 조회
    comparison_query = """
    SELECT * FROM detailed_comparison
    WHERE patent_draft_id = :draft_id
    ORDER BY detailed_comparison_total_score DESC
    """
    
    comparisons = await database.fetch_all(
        query=comparison_query,
        values={"draft_id": patent_draft_id}
    )
    
    # 적합도 결과 조회
    fitness_query = """
    SELECT * FROM fitness
    WHERE patent_draft_id = :draft_id
    ORDER BY fitness_created_at DESC
    LIMIT 1
    """
    
    fitness = await database.fetch_one(
        query=fitness_query,
        values={"draft_id": patent_draft_id}
    )
    
    # 리포트 PDF 생성
    report_data = {
        "folder": dict(folder) if folder else {},
        "draft": draft_dict,
        "similarity": dict(similarity) if similarity else {},
        "similar_patents": [dict(patent) for patent in similar_patents],
        "comparisons": [dict(comp) for comp in comparisons],
        "fitness": dict(fitness) if fitness else {}
    }
    
    # PDF 파일 생성
    pdf_path = await generate_report_pdf(report_data)
    
    # 파일 다운로드 응답
    return FileResponse(
        path=pdf_path,
        filename=f"patent_report_draft_{patent_draft_id}.pdf",
        media_type="application/pdf"
    )
