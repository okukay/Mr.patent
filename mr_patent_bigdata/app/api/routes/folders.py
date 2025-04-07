from fastapi import APIRouter, HTTPException, Depends, Query
from typing import List, Optional, Dict, Any
from datetime import datetime, timezone

from app.core.database import database
from app.schemas.patent import FolderCreate, FolderResponse, PatentDraftResponse, FolderUpdate

router = APIRouter(prefix="/fastapi", tags=["folders"])

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
    """특정 폴더에 속한 특허 초안 목록 조회"""
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
    
    # 폴더 내 특허 초안 조회
    patents_query = """
    SELECT * FROM patent_draft
    WHERE user_patent_folder_id = :folder_id
    ORDER BY patent_draft_updated_at DESC
    """
    
    patents = await database.fetch_all(
        query=patents_query,
        values={"folder_id": folder_id}
    )
    
    if not patents:
        return {
            "data": {"patents": []}
        }
    
    result = []
    for patent in patents:
        result.append({
            "patent_draft_id": patent["patent_draft_id"],
            "patent_draft_title": patent["patent_draft_title"],
            "created_at": patent["patent_draft_created_at"].isoformat() + 'Z'
        })
    
    return {
        "data": {"patents": result}
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
            "data": {"reports": []}
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
        }
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
