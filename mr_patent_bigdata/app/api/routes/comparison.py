from fastapi import APIRouter, HTTPException, Query
from typing import Dict, Any, List, Optional
import json
import logging
from datetime import datetime, timezone

from app.core.database import database
from app.core.logging import logger

router = APIRouter(prefix="/fastapi/patent", tags=["comparison"])

def get_current_timestamp():
    """현재 시간을 ISO 8601 형식으로 변환 (UTC)"""
    return datetime.now(timezone.utc).isoformat().replace('+00:00', 'Z')

@router.get("/detailed-comparison/{patent_draft_id}", response_model=Dict[str, Any])
async def get_detailed_comparison(patent_draft_id: int):
    """특허 초안과 유사한 특허들의 상세 비교 결과 조회"""
    try:
        # 특허 초안 존재 여부 확인
        draft_query = """
        SELECT patent_draft_id, patent_draft_title 
        FROM patent_draft 
        WHERE patent_draft_id = :draft_id
        """
        
        draft = await database.fetch_one(
            query=draft_query,
            values={"draft_id": patent_draft_id}
        )
        
        if not draft:
            return {
                "status": False,
                "code": 404,
                "data": None,
                "error": {
                    "code": "DRAFT_NOT_FOUND",
                    "message": "해당 특허 초안을 찾을 수 없습니다."
                }
            }
        
        # 상세 비교 결과 조회 (최적화된 쿼리)
        query = """
        SELECT dc.detailed_comparison_id, dc.similarity_patent_id, 
               dc.detailed_comparison_total_score, dc.detailed_comparison_context,
               sp.patent_id, p.patent_title, p.patent_application_number
        FROM detailed_comparison dc
        JOIN similarity_patent sp ON dc.similarity_patent_id = sp.similarity_patent_id
        JOIN patent p ON sp.patent_id = p.patent_id
        WHERE dc.patent_draft_id = :draft_id
        ORDER BY dc.detailed_comparison_total_score DESC
        """
        
        comparisons = await database.fetch_all(
            query=query,
            values={"draft_id": patent_draft_id}
        )
        
        if not comparisons:
            return {
                "data": {"comparisons": []}
            }
        
        # 결과 포맷팅
        result_comparisons = []
        for comp in comparisons:
            # JSON 문자열을 파싱 (문자열이든 dict이든 처리)
            context_data = comp["detailed_comparison_context"]
            if isinstance(context_data, str):
                context_data = json.loads(context_data)
            
            similar_contexts = []
            for context in context_data.get("highlights", []):
                similar_contexts.append({
                    "user_section": context.get("user_section", ""),
                    "patent_section": context.get("patent_section", ""),
                    "user_text": context.get("user_text", ""),
                    "patent_text": context.get("patent_text", ""),
                    "similarity_score": float(context.get("similarity_score", 0))
                })
            
            result_comparisons.append({
                "comparison_id": comp["detailed_comparison_id"],
                "similarity_patent_id": comp["similarity_patent_id"],
                "patent_id": comp["patent_id"],
                "patent_title": comp["patent_title"],
                "patent_application_number": comp["patent_application_number"],
                "detailed_comparison_total_score": float(comp["detailed_comparison_total_score"]),
                "comparison_contexts": {
                    "similar_contexts": similar_contexts
                }
            })
        
        return {
            "data": {
                "patent_draft": {
                    "id": draft["patent_draft_id"],
                    "title": draft["patent_draft_title"]
                },
                "comparisons": result_comparisons
            }
        }
    
    except Exception as e:
        logger.error(f"상세 비교 결과 조회 중 오류: {str(e)}")
        return {
            "status": False,
            "code": 500,
            "data": None,
            "error": {
                "code": "SERVER_ERROR",
                "message": f"서버 오류가 발생했습니다: {str(e)}"
            }
        }
