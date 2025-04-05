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
import platform
import tempfile
import glob
import shutil
from transformers import AutoTokenizer, AutoModel

from app.core.logging import logger
from app.core.database import database, patent
from app.services.vectorizer import get_tfidf_vector, load_vectorizer

# 전역 변수
_VECTORIZER_LOADED = False
_BERT_LOADED = False
_tokenizer = None
_model = None

# 운영체제 판별
IS_WINDOWS = platform.system() == 'Windows'

def load_bert_model():
    """KLUE BERT 모델 로드"""
    global _BERT_LOADED, _tokenizer, _model
    if not _BERT_LOADED:
        _tokenizer = AutoTokenizer.from_pretrained("klue/bert-base")
        _model = AutoModel.from_pretrained("klue/bert-base", torchscript=True)
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
    
    # TF-IDF 벡터화 UDF - Arrow 사용 안함
    @F.udf(BinaryType())  # Arrow 제거 (Windows 호환성)
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
    
    # BERT 벡터화 UDF - Arrow 사용 안함
    @F.udf(BinaryType())  # Arrow 제거 (Windows 호환성)
    def bert_vectorize(text):
        ensure_bert_loaded()
        if text is None or text == "":
            default_vector = np.zeros(768, dtype=np.float32)
            return default_vector.tobytes()
        
        # 텍스트가 너무 길면 잘라내기
        if len(text) > 1000:
            text = text[:1000]
        
        try:
            # 토큰화
            inputs = _tokenizer(text, return_tensors='pt', truncation=True, 
                               max_length=128, padding='max_length')
            
            # BERT 임베딩 생성
            with torch.no_grad():
                outputs = _model(**inputs)
                
            # CLS 토큰 임베딩 사용
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

def check_disk_usage():
    """디스크 사용량 확인 및 로깅"""
    path = "C:\\" if IS_WINDOWS else "/"
    disk = psutil.disk_usage(path)
    logger.info(f"디스크 사용량: {disk.percent}%, 사용 가능: {disk.free / (1024**3):.2f}GB")
    return disk.percent

def clean_temp_files():
    """임시 파일 정리 함수 (OS 호환)"""
    logger.info("임시 파일 정리 시작...")
    
    total_removed = 0
    
    if IS_WINDOWS:
        # Windows 환경
        temp_dir = tempfile.gettempdir()
        try:
            for pattern in [os.path.join(temp_dir, "*.tmp")]:
                for f in glob.glob(pattern):
                    try:
                        if os.path.isfile(f):
                            os.remove(f)
                            total_removed += 1
                    except Exception as e:
                        # 사용 중인 파일은 무시
                        logger.warning(f"파일 삭제 실패: {str(e)}")
        except Exception as e:
            logger.warning(f"임시 파일 정리 중 오류: {str(e)}")
    else:
        # Linux 환경
        for pattern in [
            "/tmp/ML*", "/tmp/spark*", "/tmp/blockmgr-*", 
            "/tmp/hive*", "/tmp/*.tmp"
        ]:
            try:
                for f in glob.glob(pattern):
                    try:
                        if os.path.isfile(f):
                            os.remove(f)
                            total_removed += 1
                        elif os.path.isdir(f):
                            shutil.rmtree(f, ignore_errors=True)
                            total_removed += 1
                    except Exception as e:
                        logger.warning(f"파일 삭제 실패: {str(e)}")
            except Exception as e:
                logger.warning(f"패턴 {pattern} 검색 실패: {str(e)}")
    
    logger.info(f"임시 파일 정리 완료: {total_removed}개 항목 제거됨")
    return check_disk_usage()

async def process_patents_with_spark(all_patents, batch_size=None, with_bert=False):
    """Spark를 사용한 특허 TF-IDF 벡터화 처리 (OS 최적화)"""
    
    # 환경에 따른 설정 최적화
    if batch_size is None:
        batch_size = 500 if IS_WINDOWS else 2500
    
    # 임시 디렉토리 설정
    temp_dir = os.path.join(tempfile.gettempdir(), "spark-temp") if IS_WINDOWS else "/tmp/spark-temp"
    if not os.path.exists(temp_dir):
        os.makedirs(temp_dir, exist_ok=True)
        logger.info(f"임시 디렉토리 생성됨: {temp_dir}")
    
    # 모델 디렉토리 확인
    models_dir = "models"
    if not os.path.exists(models_dir):
        os.makedirs(models_dir, exist_ok=True)
        logger.info(f"모델 디렉토리 생성됨: {models_dir}")
    
    # 시작 전 임시 파일 정리
    clean_temp_files()
    
    # 사용할 코어 수 설정
    cpu_cores = min(os.cpu_count() or 4, 4) if IS_WINDOWS else min(os.cpu_count() or 16, 16)
    
    # 메모리 설정
    driver_memory = "8g" if IS_WINDOWS else "110g"
    
    # Spark 세션 설정
    spark = SparkSession.builder \
        .appName("PatentVectorizer") \
        .config("spark.driver.memory", driver_memory) \
        .config("spark.default.parallelism", str(cpu_cores)) \
        .config("spark.sql.shuffle.partitions", str(cpu_cores * 2)) \
        .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer") \
        .config("spark.local.dir", temp_dir) \
        .master(f"local[{cpu_cores}]") \
        .getOrCreate()
    
    # 로그 레벨 설정
    spark.sparkContext.setLogLevel("ERROR")
    
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
        
        # 체크포인트 디렉토리 확인
        checkpoint_dir = os.path.dirname(checkpoint_file)
        if not os.path.exists(checkpoint_dir):
            os.makedirs(checkpoint_dir, exist_ok=True)
        
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
            # 메모리 및 디스크 사용량 확인
            memory_usage = check_memory_usage()
            disk_usage = check_disk_usage()
            
            # 디스크 용량이 80% 이상이면 정리 수행
            if disk_usage > 80:
                logger.warning(f"디스크 사용량이 높습니다 ({disk_usage}%). 임시 파일 정리 수행...")
                clean_temp_files()
            
            # 메모리 사용량이 85% 이상이면 GC 실행
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
            batch_data = []
            for p in batch_patents:
                try:
                    batch_data.append((
                        p.get("title", ""),
                        p.get("summary", ""),
                        p.get("claims", ""),
                        p.get("application_number", ""),
                        p.get("ipc_classification", "")
                    ))
                except Exception as e:
                    logger.error(f"특허 데이터 변환 오류: {str(e)}")
                    continue
            
            # 빈 배치 검사
            if not batch_data:
                logger.warning(f"배치 {batch_idx+1} 비어있음, 건너뛰기")
                continue
                
            df = spark.createDataFrame(batch_data, schema=schema)
            
            # TF-IDF 벡터화 - 한 번에 하나씩 처리
            result_df = df.withColumn("title_tfidf_vector", tfidf_vectorize(F.col("title")))
            result_df = result_df.withColumn("summary_tfidf_vector", tfidf_vectorize(F.col("summary")))
            result_df = result_df.withColumn("claim_tfidf_vector", tfidf_vectorize(F.col("claims")))
            
            # BERT 벡터화 (옵션에 따라)
            if with_bert:
                result_df = result_df.withColumn("title_bert_vector", bert_vectorize(F.col("title")))
                result_df = result_df.withColumn("summary_bert_vector", bert_vectorize(F.col("summary")))
                result_df = result_df.withColumn("claim_bert_vector", bert_vectorize(F.col("claims")))
            
            # 결과를 파이썬 객체로 변환
            try:
                patent_rows = result_df.collect()
            except Exception as e:
                logger.error(f"Spark collect 오류: {str(e)}")
                # 세션 재설정 후 다음 배치로
                spark.stop()
                await asyncio.sleep(5)
                
                spark = SparkSession.builder \
                    .appName("PatentVectorizer") \
                    .config("spark.driver.memory", driver_memory) \
                    .config("spark.default.parallelism", str(cpu_cores)) \
                    .config("spark.sql.shuffle.partitions", str(cpu_cores * 2)) \
                    .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer") \
                    .config("spark.local.dir", temp_dir) \
                    .master(f"local[{cpu_cores}]") \
                    .getOrCreate()
                    
                spark.sparkContext.setLogLevel("ERROR")
                tfidf_vectorize = create_tfidf_udf()
                
                if with_bert:
                    bert_vectorize = create_bert_udf()
                    
                continue
            
            # DB 저장 배치 크기 설정
            db_batch_size = 10 if IS_WINDOWS else 25
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
                    
                    # 값 준비
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
                    
                    # BERT 벡터 추가 (옵션에 따라)
                    if with_bert:
                        patent_data.update({
                            "patent_title_bert_vector": row.title_bert_vector,
                            "patent_summary_bert_vector": row.summary_bert_vector,
                            "patent_claim_bert_vector": row.claim_bert_vector,
                        })
                    
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
                
                # DB 배치 저장 후 메모리 정리
                db_values = None
                
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
            patent_rows = None
            result_df = None
            df = None
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
        
        # 최종 임시 파일 정리
        clean_temp_files()
    
    except Exception as e:
        logger.error(f"Spark 처리 중 오류: {str(e)}")
        raise
    finally:
        # SparkSession 종료
        spark.stop()
    
    return total_processed
