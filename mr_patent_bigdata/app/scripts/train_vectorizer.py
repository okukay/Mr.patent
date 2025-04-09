import sys
import os
import asyncio
from databases import Database
import numpy as np

# 프로젝트 루트 디렉토리를 PYTHONPATH에 추가
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '../..')))

from app.services.vectorizer import train_and_save_vectorizer, get_tfidf_vector, get_bert_vector, safe_vector
from app.core.config import settings

# 데이터베이스 연결
DATABASE_URL = settings.DATABASE_URL
database = Database(DATABASE_URL)

async def train_vectorizer_from_patents():
    await database.connect()
    
    # 특허 데이터 조회
    query = """
    SELECT patent_id, patent_title, patent_summary, patent_claim
    FROM patent
    """
    
    patents = await database.fetch_all(query=query)
    
    if not patents:
        print("학습할 특허 데이터가 없습니다.")
        return
    
    print(f"총 {len(patents)}개 특허로 벡터라이저 학습 시작...")
    
    # 1. 전체 텍스트로 TF-IDF 벡터라이저 학습
    combined_texts = []
    for patent in patents:
        title = patent["patent_title"] or ""
        summary = patent["patent_summary"] or ""
        claim = patent["patent_claim"] or ""
        combined_text = f"{title} {summary} {claim}"
        if combined_text.strip():
            combined_texts.append(combined_text)
    
    vectorizer = train_and_save_vectorizer(combined_texts)
    print("벡터라이저 학습 및 저장 완료!")
    
    # 2. 개별 특허에 대해 필드별 벡터 생성 및 저장
    for patent in patents:
        patent_id = patent["patent_id"]
        title = patent["patent_title"] or ""
        summary = patent["patent_summary"] or ""
        claim = patent["patent_claim"] or ""
        
        # 각 필드별 벡터 생성
        # 벡터 저장 전 유효성 검증 추가
        title_tfidf = safe_vector(get_tfidf_vector(title))
        summary_tfidf = safe_vector(get_tfidf_vector(summary))
        claim_tfidf = safe_vector(get_tfidf_vector(claim))
        
        # 필드별 벡터 업데이트 (통합 벡터 필드 제거)
        update_query = """
        UPDATE patent SET
            patent_title_tfidf_vector = :title_tfidf,
            patent_summary_tfidf_vector = :summary_tfidf,
            patent_claim_tfidf_vector = :claim_tfidf
        WHERE patent_id = :patent_id
        """
        
        await database.execute(
            query=update_query,
            values={
                "title_tfidf": title_tfidf.tobytes(),
                "summary_tfidf": summary_tfidf.tobytes(),
                "claim_tfidf": claim_tfidf.tobytes(),
                "patent_id": patent_id
            }
        )
    
    print("모든 특허의 필드별 벡터 업데이트 완료!")
    await database.disconnect()


if __name__ == "__main__":
    asyncio.run(train_vectorizer_from_patents())
