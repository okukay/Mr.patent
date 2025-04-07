from fastapi import APIRouter, HTTPException
from typing import Dict, Any, List
from datetime import datetime, timezone

from app.core.database import database

router = APIRouter(prefix="/fastapi/patent", tags=["comparison"])

def get_current_timestamp():
    """현재 시간을 ISO 8601 형식으로 변환 (UTC)"""
    return datetime.now(timezone.utc).isoformat().replace('+00:00', 'Z')

@router.get("/detailed-comparison/{patent_draft_id}", response_model=Dict[str, Any])
async def get_detailed_comparison(patent_draft_id: int):  # user_patent_id에서 변경
    """특허 초안과 유사한 특허들의 상세 비교 결과 조회"""
    # 해당 폴더의 상세 비교 결과 조회
    query = """
    SELECT dc.*, sp.similarity_patent_id, pp.patent_public_id
    FROM detailed_comparison dc
    JOIN similarity_patent sp ON dc.similarity_patent_id = sp.similarity_patent_id
    JOIN patent_public pp ON dc.patent_public_id = pp.patent_public_id
    WHERE dc.patent_draft_id = :draft_id  # user_patent_folder_id에서 변경
    """
    
    comparisons = await database.fetch_all(
        query=query,
        values={"draft_id": patent_draft_id}  # folder_id에서 변경
    )
    
    if not comparisons:
        return {
            "data": {"comparisons": []}
        }
    
    # 결과 포맷팅
    result_comparisons = []
    for comp in comparisons:
        # 컨텍스트 데이터 변환 (JSON에서 dict로)
        context_data = comp["detailed_comparison_context"]
        
        result_comparisons.append({
            "comparison_id": comp["detailed_comparison_id"],
            "similarity_patent_id": comp["similarity_patent_id"],
            "detailed_comparison_total_score": float(comp["detailed_comparison_total_score"]),
            "comparison_contexts": {
                "similar_contexts": [
                    {
                        "user_section": context["user_section"],
                        "patent_section": context["patent_section"],
                        "user_text": context["user_text"],
                        "patent_text": context["patent_text"],
                        "similarity_score": context["similarity_score"]
                    }
                    for context in context_data.get("highlights", [])
                ]
            }
        })
    
    return {
        "data": {
            "comparisons": result_comparisons
        }
    }
