from fastapi import APIRouter, HTTPException, Depends, Query
from typing import List, Optional, Dict, Any
from datetime import datetime, timezone
import json
import logging

from app.core.database import database
from app.schemas.patent import FolderCreate, FolderResponse, PatentDraftResponse, FolderUpdate

router = APIRouter(prefix="/fastapi", tags=["folders"])

logger = logging.getLogger(__name__)

def get_current_timestamp():
    """현재 시간을 ISO 8601 형식으로 변환 (UTC)"""
    return datetime.now(timezone.utc).isoformat().replace('+00:00', 'Z')

@router.get("/folders/{user_id}", response_model=Dict[str, Any])
async def get_user_folders(user_id: int):
    """사용자의 모든 특허 폴더 목록 조회"""
    query = """
    SELECT * FROM user_patent_folder
    WHERE user_id = :user_id
    ORDER BY user_patent_folder_updated_at DESC
    """
    
    folders = await database.fetch_all(
        query=query,
        values={"user_id": user_id}
    )
    
    if not folders:
        return {
            "data": {"folders": []}
        }
    
    result = []
    for folder in folders:
        result.append({
            "user_patent_folder_id": folder["user_patent_folder_id"],
            "user_patent_folder_title": folder["user_patent_folder_title"],
            "created_at": folder["user_patent_folder_created_at"].isoformat() + 'Z'
        })
    
    return {
        "data": {"folders": result}
    }

@router.post("/folder", response_model=Dict[str, Any])
async def create_folder(folder: FolderCreate):
    """새 특허 폴더 생성"""
    now = datetime.now(timezone.utc)
    
    query = """
    INSERT INTO user_patent_folder (
        user_id, 
        user_patent_folder_title, 
        user_patent_folder_created_at, 
        user_patent_folder_updated_at
    ) 
    VALUES (
        :user_id, 
        :folder_title, 
        :created_at, 
        :updated_at
    )
    """
    
    values = {
        "user_id": folder.user_id,
        "folder_title": folder.user_patent_folder_title,
        "created_at": now,
        "updated_at": now
    }
    
    try:
        folder_id = await database.execute(query=query, values=values)
        
        return {
            "data": {
                "user_patent_folder_id": folder_id,
                "user_patent_folder_title": folder.user_patent_folder_title,
                "created_at": now.isoformat().replace('+00:00', 'Z')
            }
        }
    except Exception as e:
        raise HTTPException(
            status_code=400,
            detail={
                "code": "INVALID_INPUT",
                "message": "폴더 생성 중 오류가 발생했습니다."
            }
        )

@router.get("/folder/{folder_id}/patents", response_model=Dict[str, Any])
async def get_folder_patents(folder_id: int):
    """폴더별 특허 초안 목록 조회"""
    try:
        # 폴더 존재 확인
        folder_query = """
        SELECT * FROM user_patent_folder WHERE user_patent_folder_id = :folder_id
        """
        folder = await database.fetch_one(
            query=folder_query,
            values={"folder_id": folder_id}
        )
        
        if not folder:
            return {
                "code": 404,
                "error": {
                    "code": "FOLDER_NOT_FOUND",
                    "message": "폴더를 찾을 수 없습니다."
                }
            }
        
        # 폴더의 특허 초안 목록 조회 (기본 정보)
        drafts_query = """
        SELECT 
            pd.patent_draft_id, 
            pd.patent_draft_title, 
            pd.patent_draft_created_at
        FROM patent_draft pd
        WHERE pd.user_patent_folder_id = :folder_id
        ORDER BY pd.patent_draft_created_at DESC
        """
        
        drafts = await database.fetch_all(
            query=drafts_query,
            values={"folder_id": folder_id}
        )
        
        # 특허 초안 목록 응답 포맷
        patents_data = []
        
        for draft in drafts:
            draft_dict = dict(draft)
            patent_draft_id = draft_dict["patent_draft_id"]
            
            # 기본값 설정 - 불리언 사용
            is_corrected = False
            total_score = 0.0  # 소수점 형태로 저장
            
            try:
                # 1. fitness 테이블에서 적합도 통과 여부 조회
                fitness_query = """
                SELECT fitness_is_corrected
                FROM fitness
                WHERE patent_draft_id = :patent_draft_id
                ORDER BY fitness_updated_at DESC
                LIMIT 1
                """
                
                fitness_data = await database.fetch_one(
                    query=fitness_query,
                    values={"patent_draft_id": patent_draft_id}
                )
                
                if fitness_data and fitness_data["fitness_is_corrected"] is not None:
                    # 0=실패, 1=통과를 불리언 값으로 변환
                    is_corrected = bool(int(fitness_data["fitness_is_corrected"]))
            except Exception as e:
                logger.error(f"Fitness 정보 조회 중 오류: {str(e)}")
                # 오류 발생 시 기본값 사용
            
            try:
                # 2. detailed_comparison 테이블에서 종합 유사도 점수 조회
                score_query = """
                SELECT detailed_comparison_total_score
                FROM detailed_comparison
                WHERE patent_draft_id = :patent_draft_id
                ORDER BY detailed_comparison_updated_at DESC
                LIMIT 1
                """
                
                score_data = await database.fetch_one(
                    query=score_query,
                    values={"patent_draft_id": patent_draft_id}
                )
                
                if score_data and score_data["detailed_comparison_total_score"] is not None:
                    # 소수점 형태 그대로 사용
                    total_score = float(score_data["detailed_comparison_total_score"])
            except Exception as e:
                logger.error(f"상세 점수 조회 중 오류: {str(e)}")
                # 오류 발생 시 기본값 사용
            
            # 날짜 형식 안전하게 처리
            created_at = draft_dict["patent_draft_created_at"]
            if isinstance(created_at, datetime):
                created_at = created_at.isoformat().replace('+00:00', 'Z')
            else:
                created_at = str(created_at) if created_at is not None else ""
            
            # 특허 초안 정보에 ERD 컬럼명과 동일한 필드명 사용
            patents_data.append({
                "patent_draft_id": patent_draft_id,
                "patent_draft_title": draft_dict["patent_draft_title"] or "",
                "fitness_is_corrected": is_corrected,  # 불리언 값 사용
                "detailed_comparison_total_score": total_score,  # 소수점 형태 사용
                "created_at": created_at
            })
        
        return {
            "data": {
                "patents": patents_data
            }
        }
    
    except Exception as e:
        # 상세 에러 로깅
        import traceback
        logger.error(f"폴더별 특허 초안 목록 조회 중 오류: {str(e)}")
        logger.error(traceback.format_exc())
        
        return {
            "code": 500,
            "error": {
                "code": "SERVER_ERROR",
                "message": f"서버 오류가 발생했습니다: {str(e)}"
            }
        }

@router.delete("/folder/{folder_id}", response_model=Dict[str, str])
async def delete_folder(folder_id: int):
    """폴더 및 폴더 내 모든 특허 초안 삭제"""
    # 트랜잭션 시작
    transaction = await database.transaction()
    
    try:
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
            await transaction.rollback()
            return {
                "status": False,
                "error": "폴더를 찾을 수 없습니다."
            }
        
        # 폴더 내 특허 초안 삭제
        delete_drafts_query = """
        DELETE FROM patent_draft
        WHERE user_patent_folder_id = :folder_id
        """
        await database.execute(
            query=delete_drafts_query,
            values={"folder_id": folder_id}
        )
        
        # 폴더 삭제
        delete_folder_query = """
        DELETE FROM user_patent_folder
        WHERE user_patent_folder_id = :folder_id
        """
        await database.execute(
            query=delete_folder_query,
            values={"folder_id": folder_id}
        )
        
        await transaction.commit()
        
        return {
            "message": "폴더가 성공적으로 삭제되었습니다."
        }
        
    except Exception as e:
        await transaction.rollback()
        return {
            "status": False,
            "error": "폴더 삭제에 실패했습니다."
        }

@router.patch("/folder/{folder_id}", response_model=Dict[str, Any])
async def update_folder_name(
    folder_id: int,
    folder_data: FolderUpdate
):
    """특허 폴더명 수정"""
    try:
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
                    "message": "지정한 폴더를 찾을 수 없습니다."
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
                "title": folder_data.user_patent_folder_title,
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
            "data": result
        }
        
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail={
                "code": "INTERNAL_ERROR",
                "message": f"폴더명 수정 중 오류가 발생했습니다: {str(e)}"
            }
        )
