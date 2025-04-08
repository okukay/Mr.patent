import findspark
findspark.init()
from pyspark.sql import SparkSession
import pyspark.sql.functions as F
from pyspark.sql.types import *
import time
import numpy as np
from datetime import datetime
import os
import gc
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
    """TF-IDF 벡터화를 위한 Spark UDF 함수 생성 (수정됨)"""
    
    # 벡터라이저 로드 함수
    def ensure_vectorizer_loaded():
        global _VECTORIZER_LOADED
        if not _VECTORIZER_LOADED:
            load_vectorizer()
            _VECTORIZER_LOADED = True
            logger.info(f"벡터라이저 로드 완료")
    
    # 내부 함수 (데코레이터 없이)
    def _tfidf_vectorize(text):
        ensure_vectorizer_loaded()
        if text is None or text == "":
            default_vector = np.zeros(1000, dtype=np.float32)
            return default_vector.tobytes()
        
        try:
            vector = get_tfidf_vector(text)
            
            # 유효성 검사 수정: 항상 벡터 반환
            if np.any(vector != 0) and len(vector) > 0:
                return vector.tobytes()
            else:
                # NULL 대신 기본 영벡터 반환
                default_vector = np.zeros(1000, dtype=np.float32)
                return default_vector.tobytes()
        except:
            default_vector = np.zeros(1000, dtype=np.float32)
            return default_vector.tobytes()
    
    # 데코레이터 적용 후 반환
    return F.udf(_tfidf_vectorize, BinaryType())

def create_bert_udf():
    """KLUE BERT 벡터화를 위한 Spark UDF 함수 생성 (수정됨)"""
    
    # BERT 모델 로드 함수
    def ensure_bert_loaded():
        if not _BERT_LOADED:
            load_bert_model()
    
    # BERT 벡터화 UDF - Arrow 사용 안함 (수정 부분)
    @F.udf(BinaryType())
    def bert_vectorize(text):
        ensure_bert_loaded()
        if text is None or text == "":
            return None  # DB에 저장되지 않도록 null 반환
        
        # 텍스트가 너무 길면 잘라내기
        if len(text) > 1000:
            text = text[:1000]
        
        try:
            inputs = _tokenizer(text, return_tensors='pt', truncation=True, 
                              max_length=128, padding='max_length')
            
            with torch.no_grad():
                outputs = _model(**inputs)
                
            sentence_embedding = outputs.last_hidden_state[:, 0, :].numpy().flatten()
            
            # 벡터 유효성 검사 추가 (핵심 수정)
            if np.any(sentence_embedding != 0) and len(sentence_embedding) > 0:
                return sentence_embedding.tobytes()
            else:
                logger.warning(f"유효하지 않은 BERT 벡터 생성됨: {text[:30]}...")
                return None
                
        except Exception as e:
            logger.error(f"BERT 벡터 생성 중 오류: {str(e)}")
            return None
    
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
    """Spark를 사용한 특허 벡터화 처리 (최종 수정 버전)"""
    # 환경 설정
    if batch_size is None:
        batch_size = 500 if IS_WINDOWS else 2500
    
    # 디렉토리 설정
    temp_dir = os.path.join(tempfile.gettempdir(), "spark-temp") if IS_WINDOWS else "/tmp/spark-temp"
    os.makedirs(temp_dir, exist_ok=True)
    os.makedirs("models", exist_ok=True)
    
    # 초기 리소스 정리
    clean_temp_files()
    
    # 시스템 설정
    cpu_cores = min(os.cpu_count() or 4, 4) if IS_WINDOWS else min(os.cpu_count() or 16, 16)
    driver_memory = "8g" if IS_WINDOWS else "110g"
    db_batch_size = 10 if IS_WINDOWS else 25
    
    # Spark 세션 초기화
    spark = initialize_spark_session(temp_dir, cpu_cores, driver_memory)
    
    # UDF 함수 생성
    tfidf_vectorize = create_tfidf_udf()
    bert_vectorize = create_bert_udf() if with_bert else None
    
    try:
        # 작업 초기화
        start_time = time.time()
        total_processed = 0
        total_patents = len(all_patents)
        num_batches = (total_patents + batch_size - 1) // batch_size
        
        # 체크포인트 설정
        checkpoint_file = os.path.join(temp_dir, "patent_processing_checkpoint.txt")
        os.makedirs(os.path.dirname(checkpoint_file), exist_ok=True)
        last_processed_batch = load_checkpoint(checkpoint_file)
        
        # 스키마 정의
        schema = StructType([
            StructField("patent_id", LongType(), True),
            StructField("title", StringType(), True),
            StructField("summary", StringType(), True),
            StructField("claims", StringType(), True),
            StructField("application_number", StringType(), True),
            StructField("ipc_classification", StringType(), True)
        ])
        
        # 벡터 유효성 검사 강화 (핵심 수정)
        for batch_idx in range(last_processed_batch, num_batches):
            await manage_resources()
            
            batch_start = time.time()
            start_idx = batch_idx * batch_size
            end_idx = min((batch_idx + 1) * batch_size, total_patents)
            batch_patents = all_patents[start_idx:end_idx]
            
            logger.info(f"배치 {batch_idx+1}/{num_batches} 처리 중 ({len(batch_patents)}개 특허)")
            
            # 벡터 유효성 검사 강화 - 수정된 부분
            valid_patents = []
            # 특허 필터링 기준 완화
            for p in batch_patents:
                # 세 필드 중 하나라도 의미 있는 길이면 포함
                if any(len(str(field)) > 10 for field in [p["patent_title"], p["patent_summary"], p["patent_claim"]]):
                    valid_patents.append(p)
                else:
                    logger.warning(f"유효하지 않은 특허 데이터 건너뜀: {p['patent_title'][:30] if p['patent_title'] else ''}...")
            
            batch_data = prepare_batch_data(valid_patents, schema)
            
            if not batch_data:
                logger.warning(f"배치 {batch_idx+1} 유효한 데이터 없음")
                continue
                
            df = spark.createDataFrame(batch_data, schema=schema)
            result_df = vectorize_patents(df, tfidf_vectorize, bert_vectorize)
            
            try:
                patent_rows = result_df.collect()
            except Exception as e:
                logger.error(f"Spark collect 오류: {str(e)}")
                spark.stop()
                await asyncio.sleep(5)
                spark = initialize_spark_session(temp_dir, cpu_cores, driver_memory)
                tfidf_vectorize = create_tfidf_udf()
                if with_bert:
                    bert_vectorize = create_bert_udf()
                continue
            
            processed = await save_to_database(patent_rows, with_bert, db_batch_size)
            total_processed += processed
            
            batch_duration = time.time() - batch_start
            patents_per_second = len(batch_patents) / batch_duration if batch_duration > 0 else 0
            logger.info(f"배치 {batch_idx+1}/{num_batches} 완료: {len(batch_patents)}개 처리 ({batch_duration:.2f}초)")
            
            save_checkpoint(checkpoint_file, batch_idx)
            patent_rows = None
            result_df = None
            df = None
            gc.collect()
            if torch.cuda.is_available():
                torch.cuda.empty_cache()
            
            await asyncio.sleep(2)
        
        total_duration = time.time() - start_time
        logger.info(f"총 {total_processed}개 특허 처리 완료 ({total_duration/60:.2f}분)")
        
        if os.path.exists(checkpoint_file):
            os.remove(checkpoint_file)
        clean_temp_files()
        
        return total_processed
        
    except Exception as e:
        logger.error(f"처리 중 치명적 오류: {str(e)}")
        raise
    finally:
        spark.stop()

# 1. Spark 세션 초기화 함수
def initialize_spark_session(temp_dir, cpu_cores, driver_memory="8g"):
    """Spark 세션을 초기화하고 반환합니다."""
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
    return spark

# 2. 체크포인트 관리 함수들
def load_checkpoint(checkpoint_file):
    """체크포인트 파일에서 마지막으로 처리된 배치 번호를 로드합니다."""
    if os.path.exists(checkpoint_file):
        with open(checkpoint_file, 'r') as f:
            try:
                last_processed_batch = int(f.read().strip())
                logger.info(f"체크포인트 발견: 배치 {last_processed_batch}부터 재개합니다")
                return last_processed_batch
            except:
                return 0
    return 0

def save_checkpoint(checkpoint_file, batch_idx):
    """현재 배치 번호를 체크포인트 파일에 저장합니다."""
    with open(checkpoint_file, 'w') as f:
        f.write(str(batch_idx + 1))

# 3. 배치 데이터 변환 함수
def prepare_batch_data(batch_patents, schema):
    """특허 배치 데이터를 Spark DataFrame으로 변환할 수 있는 형식으로 준비합니다."""
    batch_data = []
    for p in batch_patents:
        try:
            batch_data.append((
                p["patent_id"],
                p["patent_title"],
                p["patent_summary"],
                p["patent_claim"],
                p["patent_application_number"],
                p["patent_ipc"]
            ))
        except Exception as e:
            logger.error(f"특허 데이터 변환 오류: {str(e)}")
    return batch_data

# 4. 벡터화 함수
def vectorize_patents(df, tfidf_vectorize, bert_vectorize=None):
    """특허 DataFrame에 벡터화를 적용합니다."""
    # patent_id 컬럼을 보존하기 위해 먼저 임시 변수에 저장
    patent_ids = df.select("patent_id").collect()
    
    # TF-IDF 벡터화
    result_df = df.withColumn("title_tfidf_vector", tfidf_vectorize(F.col("title")))
    result_df = result_df.withColumn("summary_tfidf_vector", tfidf_vectorize(F.col("summary")))
    result_df = result_df.withColumn("claim_tfidf_vector", tfidf_vectorize(F.col("claims")))
    
    # BERT 벡터화 (선택적)
    if bert_vectorize is not None:
        result_df = result_df.withColumn("title_bert_vector", bert_vectorize(F.col("title")))
        result_df = result_df.withColumn("summary_bert_vector", bert_vectorize(F.col("summary")))
        result_df = result_df.withColumn("claim_bert_vector", bert_vectorize(F.col("claims")))
    
    return result_df


# 5. 데이터베이스 저장 함수 (수정됨)
async def save_to_database(patent_rows, with_bert=False, db_batch_size=25):
    """벡터화된 특허 데이터의 벡터 필드만 업데이트합니다."""
    total_processed = 0
    db_batches = [patent_rows[i:i+db_batch_size] for i in range(0, len(patent_rows), db_batch_size)]
    
    for db_idx, db_batch in enumerate(db_batches):
        for row in db_batch:
            try:
                # 벡터 유효성 검사
                valid_vectors = (
                    row.title_tfidf_vector is not None and len(row.title_tfidf_vector) > 0 and
                    row.summary_tfidf_vector is not None and len(row.summary_tfidf_vector) > 0 and
                    row.claim_tfidf_vector is not None and len(row.claim_tfidf_vector) > 0
                )
                
                if valid_vectors and hasattr(row, "patent_id") and row.patent_id:
                    # UPDATE 쿼리 실행
                    update_query = """
                    UPDATE patent
                    SET patent_title_tfidf_vector = :title_vector,
                        patent_summary_tfidf_vector = :summary_vector,
                        patent_claim_tfidf_vector = :claim_vector,
                        patent_updated_at = :updated_at
                    WHERE patent_id = :patent_id
                    """
                    
                    await database.execute(
                        query=update_query,
                        values={
                            "title_vector": row.title_tfidf_vector,
                            "summary_vector": row.summary_tfidf_vector,
                            "claim_vector": row.claim_tfidf_vector,
                            "updated_at": datetime.utcnow(),
                            "patent_id": row.patent_id
                        }
                    )
                    total_processed += 1
                else:
                    logger.warning(f"유효한 벡터 또는 특허 ID가 없음: {row.title[:30] if hasattr(row, 'title') else 'unknown'}")
            except Exception as e:
                logger.error(f"특허 업데이트 오류: {str(e)}")
        
        # 진행상황 로깅
        if db_idx % 10 == 0 or db_idx == len(db_batches) - 1:
            logger.info(f"DB 업데이트 진행: {db_idx+1}/{len(db_batches)} 배치 완료 (총 {total_processed}개 처리)")
        
        await asyncio.sleep(0.5)
    
    return total_processed

# 6. 데이터 전처리 헬퍼 함수
def prepare_patent_data(row, with_bert=False):
    """특허 행 데이터를 데이터베이스 저장 형식으로 변환합니다."""
    # 출원번호 정제
    app_number = row.application_number
    if app_number and "발명의명칭" in app_number:
        app_number = app_number.replace("발명의명칭", "")
    
    # IPC 코드 길이 제한
    ipc_code = row.ipc_classification
    if ipc_code and len(ipc_code) > 95:
        ipc_code = ipc_code[:95]
    
    # 기본 데이터
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
    
    # BERT 벡터 추가 (선택적)
    if with_bert:
        patent_data.update({
            "patent_title_bert_vector": row.title_bert_vector,
            "patent_summary_bert_vector": row.summary_bert_vector,
            "patent_claim_bert_vector": row.claim_bert_vector,
        })
    
    return patent_data

# 7. 리소스 관리 함수
async def manage_resources():
    """메모리와 디스크 상태를 확인하고 필요시 리소스를 정리합니다."""
    memory_usage = check_memory_usage()
    disk_usage = check_disk_usage()
    
    # 디스크 용량 확인 및 정리
    if disk_usage > 80:
        logger.warning(f"디스크 사용량이 높습니다 ({disk_usage}%). 임시 파일 정리 수행...")
        clean_temp_files()
    
    # 메모리 사용량 확인 및 정리
    if memory_usage > 85:
        logger.warning(f"메모리 사용량이 높습니다 ({memory_usage}%). 잠시 대기 후 GC 실행...")
        await asyncio.sleep(5)
        import gc
        gc.collect()
        if torch.cuda.is_available():
            torch.cuda.empty_cache()
