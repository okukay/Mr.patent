import findspark
findspark.init()
from pyspark.sql import SparkSession
import pyspark.sql.functions as F
from pyspark.sql.types import *
import time
import numpy as np
from datetime import datetime
import os
import torch
import asyncio
import psutil
from transformers import AutoTokenizer, AutoModel

from app.core.logging import logger
from app.core.database import database, patent
from app.services.vectorizer import get_tfidf_vector, load_vectorizer

# 전역 변수
_VECTORIZER_LOADED = False
_BERT_LOADED = False
_tokenizer = None
_model = None

def load_bert_model():
    """KLUE BERT 모델 로드"""
    global _BERT_LOADED, _tokenizer, _model
    if not _BERT_LOADED:
        _tokenizer = AutoTokenizer.from_pretrained("klue/bert-base")
        _model = AutoModel.from_pretrained("klue/bert-base", torchscript=True)  # torchscript=True 추가
        _BERT_LOADED = True
        logger.info("KLUE/BERT 모델 로드 완료")

def create_tfidf_udf():
    """TF-IDF 벡터화를 위한 Spark UDF 함수 생성"""
    
    # 벡터라이저 로드 함수
    def ensure_vectorizer_loaded():
        global _VECTORIZER_LOADED
        if not _VECTORIZER_LOADED:
            load_vectorizer()
            _VECTORIZER_LOADED = True
            logger.info(f"벡터라이저 로드 완료")
    
    # TF-IDF 벡터화 UDF - useArrow 제거
    @F.udf(BinaryType())
    def tfidf_vectorize(text):
        ensure_vectorizer_loaded()
        if text is None or text == "":
            default_vector = np.zeros(1000, dtype=np.float32)
            return default_vector.tobytes()
        
        vector = get_tfidf_vector(text)
        return vector.tobytes()
    
    return tfidf_vectorize

def create_bert_udf():
    """KLUE BERT 벡터화를 위한 Spark UDF 함수 생성"""
    
    # BERT 모델 로드 함수
    def ensure_bert_loaded():
        if not _BERT_LOADED:
            load_bert_model()
    
    # BERT 벡터화 UDF - useArrow 제거
    @F.udf(BinaryType())
    def bert_vectorize(text):
        ensure_bert_loaded()
        if text is None or text == "":
            default_vector = np.zeros(768, dtype=np.float32)
            return default_vector.tobytes()
        
        # 텍스트가 너무 길면 잘라내기 (1000자로 축소)
        if len(text) > 1000:
            text = text[:1000]
        
        try:
            # 토큰화 - max_length를 128로 축소
            inputs = _tokenizer(text, return_tensors='pt', truncation=True, 
                               max_length=128, padding='max_length')
            
            # BERT 임베딩 생성
            with torch.no_grad():
                outputs = _model(**inputs)
                
            # CLS 토큰 임베딩 사용 (문장 벡터)
            sentence_embedding = outputs.last_hidden_state[:, 0, :].numpy().flatten()
            return sentence_embedding.tobytes()
        except Exception as e:
            logger.error(f"BERT 벡터 생성 중 오류: {str(e)}")
            default_vector = np.zeros(768, dtype=np.float32)
            return default_vector.tobytes()
    
    return bert_vectorize

def check_memory_usage():
    """메모리 사용량 확인 및 로깅"""
    mem = psutil.virtual_memory()
    logger.info(f"메모리 사용량: {mem.percent}%, 사용 가능: {mem.available / (1024**3):.2f}GB")
    return mem.percent

async def process_patents_with_spark(all_patents, batch_size=2500, with_bert=False):  # 배치 크기를 1000으로 증가
    """Spark를 사용한 특허 TF-IDF 벡터화 처리 (BERT는 별도 처리)
    
    Args:
        all_patents: 처리할 특허 데이터 목록
        batch_size: 배치 크기 (기본값: 1000)
        with_bert: BERT 벡터화 함께 수행 여부 (기본값: False)
    """
    # 임시 디렉토리 확인 및 생성
    temp_dir = "/tmp/spark-temp"  # Linux 경로로 변경
    if not os.path.exists(temp_dir):
        os.makedirs(temp_dir, exist_ok=True)
        logger.info(f"임시 디렉토리 생성됨: {temp_dir}")
    
    # Spark 세션 설정 - EC2 인스턴스에 최적화
    spark = SparkSession.builder \
        .appName("PatentVectorizer") \
        .config("spark.driver.memory", "110g") \
        .config("spark.sql.execution.arrow.maxRecordsPerBatch", "10000") \
        .config("spark.default.parallelism", "32") \
        .config("spark.sql.shuffle.partitions", "200") \
        .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer") \
        .master("local[16]") \
        .getOrCreate()
    
    # TF-IDF 및 BERT 벡터화 UDF 함수 생성
    tfidf_vectorize = create_tfidf_udf()
    bert_vectorize = None
    if with_bert:
        bert_vectorize = create_bert_udf()
    
    try:
        start_time = time.time()
        total_processed = 0
        total_patents = len(all_patents)
        num_batches = (total_patents + batch_size - 1) // batch_size
        
        vectorization_type = "TF-IDF 및 BERT" if with_bert else "TF-IDF"
        logger.info(f"총 {total_patents}개 특허를 {num_batches}개 배치로 {vectorization_type} 벡터화 처리 (배치 크기: {batch_size})")
        
        # 체크포인트 파일 경로
        checkpoint_file = os.path.join(temp_dir, "patent_processing_checkpoint.txt")
        last_processed_batch = 0
        
        # 이전 체크포인트 확인
        if os.path.exists(checkpoint_file):
            with open(checkpoint_file, 'r') as f:
                try:
                    last_processed_batch = int(f.read().strip())
                    logger.info(f"체크포인트 발견: 배치 {last_processed_batch}부터 재개합니다")
                except:
                    last_processed_batch = 0
        
        # 특허 데이터 스키마 정의
        schema = StructType([
            StructField("title", StringType(), True),
            StructField("summary", StringType(), True),
            StructField("claims", StringType(), True),
            StructField("application_number", StringType(), True),
            StructField("ipc_classification", StringType(), True)
        ])
        
        # 특허 데이터를 배치로 처리
        for batch_idx in range(last_processed_batch, num_batches):
            # 메모리 사용량 확인
            memory_usage = check_memory_usage()
            if memory_usage > 85:
                logger.warning(f"메모리 사용량이 높습니다 ({memory_usage}%). 잠시 대기 후 GC 실행...")
                await asyncio.sleep(5)
                import gc
                gc.collect()
                if torch.cuda.is_available():
                    torch.cuda.empty_cache()
            
            batch_start = time.time()
            start_idx = batch_idx * batch_size
            end_idx = min((batch_idx + 1) * batch_size, total_patents)
            batch_patents = all_patents[start_idx:end_idx]
            
            logger.info(f"배치 {batch_idx+1}/{num_batches} 처리 중 ({len(batch_patents)}개 특허)")
            
            # 배치를 Spark DataFrame으로 변환
            batch_data = [(
                p.get("title", ""),
                p.get("summary", ""),
                p.get("claims", ""),
                p.get("application_number", ""),
                p.get("ipc_classification", "")
            ) for p in batch_patents]
            
            df = spark.createDataFrame(batch_data, schema=schema)
            
            # TF-IDF 벡터화 적용
            result_df = df.withColumn("title_tfidf_vector", tfidf_vectorize(F.col("title"))) \
                          .withColumn("summary_tfidf_vector", tfidf_vectorize(F.col("summary"))) \
                          .withColumn("claim_tfidf_vector", tfidf_vectorize(F.col("claims")))
            
            # BERT 벡터화 추가 적용 (옵션에 따라)
            if with_bert:
                result_df = result_df.withColumn("title_bert_vector", bert_vectorize(F.col("title"))) \
                                    .withColumn("summary_bert_vector", bert_vectorize(F.col("summary"))) \
                                    .withColumn("claim_bert_vector", bert_vectorize(F.col("claims")))
            
            # 결과를 파이썬 객체로 변환
            patent_rows = result_df.collect()
            
            # 데이터베이스에 더 작은 배치 단위로 저장 (메모리 부담 감소)
            db_batch_size = 20  # 더 작게 설정
            db_batches = [patent_rows[i:i+db_batch_size] for i in range(0, len(patent_rows), db_batch_size)]
            
            for db_idx, db_batch in enumerate(db_batches):
                db_values = []
                for row in db_batch:
                    # 출원번호 정제
                    app_number = row.application_number
                    if app_number and "발명의명칭" in app_number:
                        app_number = app_number.replace("발명의명칭", "")
                    
                    # IPC 코드 길이 제한
                    ipc_code = row.ipc_classification
                    if ipc_code and len(ipc_code) > 95:
                        ipc_code = ipc_code[:95]
                    
                    # 값 준비 (TF-IDF 벡터만 포함)
                    patent_data = {
                        "patent_title": row.title,
                        "patent_application_number": app_number,
                        "patent_ipc": ipc_code,
                        "patent_summary": row.summary,
                        "patent_claim": row.claims,
                        "patent_title_tfidf_vector": row.title_tfidf_vector,
                        "patent_summary_tfidf_vector": row.summary_tfidf_vector,
                        "patent_claim_tfidf_vector": row.claim_tfidf_vector,
                        "patent_created_at": datetime.utcnow(),
                        "patent_updated_at": datetime.utcnow()
                    }
                    
                    db_values.append(patent_data)
                
                try:
                    # 배치 삽입 실행
                    query = patent.insert().values(db_values)
                    await database.execute(query)
                    total_processed += len(db_values)
                    
                    # 중간 저장 진행 상황 로깅
                    if db_idx % 5 == 0 or db_idx == len(db_batches) - 1:
                        logger.info(f"배치 {batch_idx+1}/{num_batches} 중 DB 저장 진행: {db_idx+1}/{len(db_batches)} 완료")
                
                except Exception as e:
                    logger.error(f"배치 삽입 중 오류: {str(e)}")
                    # 개별 삽입으로 대체
                    for item in db_values:
                        try:
                            query = patent.insert().values(item)
                            await database.execute(query)
                            total_processed += 1
                        except Exception as inner_e:
                            logger.error(f"개별 삽입 중 오류: {str(inner_e)}")
                
                # DB 배치 간 짧은 대기 추가
                await asyncio.sleep(0.5)
            
            # 성능 지표 계산 및 로깅
            batch_duration = time.time() - batch_start
            patents_per_second = len(batch_patents) / batch_duration if batch_duration > 0 else 0
            
            logger.info(f"배치 {batch_idx+1}/{num_batches} 완료: {len(batch_patents)}개 특허를 {batch_duration:.2f}초에 처리 ({patents_per_second:.2f} 특허/초)")
            
            # 예상 남은 시간 계산
            elapsed_time = time.time() - start_time
            avg_time_per_batch = elapsed_time / (batch_idx + 1 - last_processed_batch)
            remaining_batches = num_batches - (batch_idx + 1)
            est_time_remaining = avg_time_per_batch * remaining_batches
            
            logger.info(f"진행 상황: {batch_idx+1}/{num_batches} 배치. 예상 남은 시간: {est_time_remaining/60:.2f}분")
            
            # 체크포인트 저장
            with open(checkpoint_file, 'w') as f:
                f.write(str(batch_idx + 1))
            
            # 메모리 정리를 위해 배치마다 GC 실행
            import gc
            gc.collect()
            if torch.cuda.is_available():
                torch.cuda.empty_cache()
            
            # 배치 사이에 짧은 휴식
            await asyncio.sleep(2)
        
        total_duration = time.time() - start_time
        logger.info(f"Spark {vectorization_type} 처리 완료: {total_processed}개 특허를 {total_duration/60:.2f}분에 처리")
        
        # 처리 완료 후 체크포인트 파일 삭제
        if os.path.exists(checkpoint_file):
            os.remove(checkpoint_file)
    
    except Exception as e:
        logger.error(f"Spark 처리 중 오류: {str(e)}")
        raise
    finally:
        # SparkSession 종료
        spark.stop()
    
    return total_processed
