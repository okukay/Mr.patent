import sys
import os
import asyncio
from databases import Database
import numpy as np
from konlpy.tag import Okt
from sklearn.feature_extraction.text import TfidfVectorizer

from app.services.vectorizer import train_and_save_vectorizer, get_tfidf_vector, safe_vector, save_vectorizer
from app.core.config import settings

# 프로젝트 루트 디렉토리를 PYTHONPATH에 추가
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '../..')))

# 데이터베이스 연결
DATABASE_URL = settings.DATABASE_URL
database = Database(DATABASE_URL)

# 한국어 토큰화 함수
def tokenize_korean(text):
    okt = Okt()
    return okt.morphs(text)

async def train_vectorizer_from_patents():
    database = Database(settings.DATABASE_URL)
    await database.connect()
    
    # 한국어 특허 데이터 조회 (한국어 특허만 필터링하는 조건 추가 가능)
    query = """
    SELECT patent_id, patent_title, patent_summary, patent_claim
    FROM patent
    """
    
    patents = await database.fetch_all(query=query)
    
    if not patents:
        print("학습할 특허 데이터가 없습니다.")
        return
    
    print(f"총 {len(patents)}개 특허로 한국어 벡터라이저 학습 시작...")
    
    # 한국어 처리에 적합한 벡터라이저 생성
    korean_vectorizer = TfidfVectorizer(
        max_features=1000,
        tokenizer=tokenize_korean,
        analyzer='word'
    )
    
    # 학습 데이터 준비
    combined_texts = []
    for patent in patents:
        title = patent["patent_title"] or ""
        summary = patent["patent_summary"] or ""
        claim = patent["patent_claim"] or ""
        combined_text = f"{title} {summary} {claim}"
        if combined_text.strip():
            combined_texts.append(combined_text)
    
    # 벡터라이저 학습
    korean_vectorizer.fit(combined_texts)
    
    # 학습된 벡터라이저 저장
    save_vectorizer(korean_vectorizer)
    
    print(f"한국어 TF-IDF 벡터라이저 학습 완료 (어휘 크기: {len(korean_vectorizer.vocabulary_)})")
    await database.disconnect()

if __name__ == "__main__":
    asyncio.run(train_vectorizer_from_patents())
