import numpy as np
import torch
from sklearn.feature_extraction.text import TfidfVectorizer
from typing import List, Dict, Any, Tuple
from kobert_tokenizer import KoBERTTokenizer
from transformers import BertModel

from app.core.logging import logger

# TF-IDF 벡터라이저와 KoBERT 모델 초기화
tfidf_vectorizer = TfidfVectorizer(max_features=1000)

# KoBERT 모델 초기화
tokenizer = KoBERTTokenizer.from_pretrained('skt/kobert-base-v1')
model = BertModel.from_pretrained('skt/kobert-base-v1', return_dict=False)

def fit_tfidf_vectorizer(corpus: List[str]) -> None:
    """TF-IDF 벡터라이저 학습"""
    global tfidf_vectorizer
    tfidf_vectorizer = TfidfVectorizer(max_features=1000)
    tfidf_vectorizer.fit(corpus)
    logger.info("TF-IDF 벡터라이저 학습 완료")

def get_tfidf_vector(text: str) -> np.ndarray:
    """텍스트의 TF-IDF 벡터 계산"""
    if not text:
        return np.zeros(1000)
    return tfidf_vectorizer.transform([text]).toarray()[0]

def get_kobert_vector(text: str, max_length: int = 512) -> np.ndarray:
    """텍스트의 KoBERT 벡터 계산"""
    if not text:
        return np.zeros(768)  # KoBERT 임베딩 차원
    
    # 텍스트가 너무 길면 잘라내기
    if len(text) > max_length * 4:
        text = text[:max_length * 4]
    
    inputs = tokenizer(text, return_tensors='pt', truncation=True, max_length=max_length, padding='max_length')
    with torch.no_grad():
        # return_dict=True를 추가하여 속성으로 접근 가능한 객체 반환
        outputs = model(**inputs, return_dict=True)
    
    return outputs.last_hidden_state[:, 0, :].numpy().flatten()

def generate_vectors(patent_data: Dict[str, Any]) -> Tuple[np.ndarray, np.ndarray]:
    """특허 데이터의 벡터 생성"""
    title = patent_data.get("title", "")
    summary = patent_data.get("summary", "")
    claims = patent_data.get("claims", "")
    
    combined_text = f"{title} {summary} {claims}"
    
    tfidf_vector = get_tfidf_vector(combined_text)
    kobert_vector = get_kobert_vector(combined_text)
    
    return tfidf_vector, kobert_vector
