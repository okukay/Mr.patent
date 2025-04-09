import os
import pickle
import numpy as np
import torch
from sklearn.feature_extraction.text import TfidfVectorizer
from typing import List, Dict, Any, Tuple
from transformers import AutoTokenizer, AutoModel

from app.core.logging import logger

# 벡터라이저 저장 경로
VECTORIZER_PATH = "models/tfidf_vectorizer.pkl"
os.makedirs(os.path.dirname(VECTORIZER_PATH), exist_ok=True)

# TF-IDF 벡터라이저 초기화
tfidf_vectorizer = TfidfVectorizer(max_features=1000)

# KLUE BERT 모델 초기화
tokenizer = None
model = None
BERT_LOADED = False

def load_bert_model():
    """KLUE BERT 모델 로드"""
    global tokenizer, model, BERT_LOADED
    if not BERT_LOADED:
        tokenizer = AutoTokenizer.from_pretrained("klue/bert-base")
        model = AutoModel.from_pretrained("klue/bert-base")
        BERT_LOADED = True
        logger.info("KLUE/BERT 모델 로드 완료")

def train_and_save_vectorizer(corpus, filename=VECTORIZER_PATH):
    """특허 말뭉치로 TF-IDF 벡터라이저 학습 및 저장"""
    global tfidf_vectorizer
    
    # 벡터라이저 학습
    tfidf_vectorizer.fit(corpus)
    
    # 학습된 벡터라이저 저장
    with open(filename, "wb") as f:
        pickle.dump(tfidf_vectorizer, f)
    
    print(f"TF-IDF 벡터라이저 학습 완료 (어휘 크기: {len(tfidf_vectorizer.vocabulary_)})")
    return tfidf_vectorizer

def load_vectorizer(filename=VECTORIZER_PATH):
    """저장된 TF-IDF 벡터라이저 로드"""
    global tfidf_vectorizer
    
    try:
        with open(filename, "rb") as f:
            tfidf_vectorizer = pickle.load(f)
        print(f"TF-IDF 벡터라이저 로드 완료 (어휘 크기: {len(tfidf_vectorizer.vocabulary_)})")
    except FileNotFoundError:
        print("저장된 벡터라이저가 없습니다. 먼저 학습을 진행하세요.")
    
    return tfidf_vectorizer

def fit_tfidf_vectorizer(corpus: List[str]) -> None:
    """TF-IDF 벡터라이저 학습"""
    global tfidf_vectorizer
    
    # 기존 벡터라이저 재사용 (새로 생성하지 않음)
    tfidf_vectorizer.fit(corpus)
    logger.info("TF-IDF 벡터라이저 학습 완료")
    
    # 학습된 벡터라이저 저장
    save_vectorizer(tfidf_vectorizer)

def save_vectorizer(vectorizer, filename=VECTORIZER_PATH):
    """학습된 TF-IDF 벡터라이저 저장"""
    with open(filename, "wb") as f:
        pickle.dump(vectorizer, f)
    logger.info(f"TF-IDF 벡터라이저 저장 완료 (경로: {filename})")

def get_tfidf_vector(text: str) -> np.ndarray:
    """텍스트의 TF-IDF 벡터 계산"""
    global tfidf_vectorizer
    
    if not text:
        return np.zeros(1000)
    
    try:
        # 벡터라이저 사용하여 변환
        return tfidf_vectorizer.transform([text]).toarray()[0]
    except Exception as e:
        logger.error(f"TF-IDF 벡터 생성 중 오류: {e}")
        # 저장된 벡터라이저 로드 시도
        try:
            load_vectorizer()
            return tfidf_vectorizer.transform([text]).toarray()[0]
        except:
            # 빈 벡터 반환
            logger.warning("벡터라이저 로드 실패. 영벡터를 반환합니다.")
            return np.zeros(1000)
        
# vectorizer.py에 추가
def safe_vector(vec, dim=1000):
    """안전한 벡터 반환 (NaN, 영벡터 처리)"""
    if vec is None or (isinstance(vec, np.ndarray) and (np.all(vec == 0) or np.isnan(vec).any())):
        # 랜덤 소음 추가 (정규분포, 작은 표준편차)
        return np.random.normal(0, 0.01, dim)
    return vec

def get_bert_vector(text: str, max_length: int = 512) -> np.ndarray:
    """텍스트의 KLUE BERT 벡터 계산"""
    global tokenizer, model, BERT_LOADED
    
    if not text:
        return np.zeros(768)  # BERT 임베딩 차원
    
    # BERT 모델 로드 확인
    if not BERT_LOADED:
        load_bert_model()
    
    try:
        # 텍스트가 너무 길면 잘라내기
        if len(text) > max_length * 4:
            text = text[:max_length * 4]
        
        inputs = tokenizer(text, return_tensors='pt', truncation=True, max_length=max_length, padding='max_length')
        with torch.no_grad():
            outputs = model(**inputs)
        
        # CLS 토큰 임베딩 사용
        return outputs.last_hidden_state[:, 0, :].numpy().flatten()
    except Exception as e:
        logger.error(f"BERT 벡터 생성 중 오류: {e}")
        return np.zeros(768)  # 오류 발생 시 영벡터 반환

def generate_vectors(patent_data, with_bert=False):
    """특허 데이터의 필드별 벡터 생성"""
    title = patent_data.get("title", "")
    summary = patent_data.get("summary", "")
    claims = patent_data.get("claims", "")
    
    # 필드별 TF-IDF 벡터 생성
    title_tfidf = get_tfidf_vector(title)
    summary_tfidf = get_tfidf_vector(summary)
    claim_tfidf = get_tfidf_vector(claims)
    
    result = {
        'title_tfidf_vector': title_tfidf,
        'summary_tfidf_vector': summary_tfidf,
        'claim_tfidf_vector': claim_tfidf
    }
    
    # BERT 벡터는 선택적으로 생성 (일반적으로 필요 없음)
    if with_bert:
        title_bert = get_bert_vector(title)
        summary_bert = get_bert_vector(summary)
        claim_bert = get_bert_vector(claims)
        
        result.update({
            'title_bert_vector': title_bert,
            'summary_bert_vector': summary_bert,
            'claim_bert_vector': claim_bert
        })
    
    return result

def generate_field_vectors(field_text: str, with_bert=False) -> Tuple[np.ndarray, np.ndarray]:
    """특정 필드의 벡터 생성"""
    tfidf_vector = get_tfidf_vector(field_text)
    
    # BERT 벡터는 선택적으로 생성
    if with_bert:
        bert_vector = get_bert_vector(field_text)
        return tfidf_vector, bert_vector
    else:
        # TF-IDF 벡터만 반환
        return tfidf_vector, None
