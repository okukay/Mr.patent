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
_VECTORIZER_INSTANCE = None  # 추가: 벡터라이저 인스턴스 캐싱

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
    """TF-IDF 벡터화를 위한 Spark UDF 함수 생성 (최적화)"""
    
    # 벡터라이저 로드 함수 (최적화)
    def ensure_vectorizer_loaded():
        global _VECTORIZER_LOADED, _VECTORIZER_INSTANCE
        if not _VECTORIZER_LOADED:
            _VECTORIZER_INSTANCE = load_vectorizer()
            _VECTORIZER_LOADED = True
            logger.info(f"벡터라이저 로드 완료")
    
    # 내부 함수 (최적화)
    def _tfidf_vectorize(text):
        ensure_vectorizer_loaded()
        if text is None or text == "":
            default_vector = np.zeros(1000, dtype=np.float32)
            return default_vector.tobytes()
        
        try:
            vector = get_tfidf_vector(text)
            return vector.tobytes() if np.any(vector != 0) and len(vector) > 0 else np.zeros(1000, dtype=np.float32).tobytes()
        except:
            return np.zeros(1000, dtype=np.float32).tobytes()
    
    # 데코레이터 적용 후 반환
    return F.udf(_tfidf_vectorize, BinaryType())

def create_bert_udf():
    """KLUE BERT 벡터화를 위한 Spark UDF 함수 생성 (변경 없음)"""
    
    # BERT 모델 로드 함수
    def ensure_bert_loaded():
        if not _BERT_LOADED:
            load_bert_model()
    
    # BERT 벡터화 UDF
    @F.udf(BinaryType())
    def bert_vectorize(text):
        ensure_bert_loaded()
        if text is None or text == "":
            return None
        
        # 텍스트가 너무 길면 잘라내기
        if len(text) > 1000:
            text = text[:1000]
        
        try:
            inputs = _tokenizer(text, return_tensors='pt', truncation=True, 
                              max_length=128, padding='max_length')
            
            with torch.no_grad():
                outputs = _model(**inputs)
                
            sentence_embedding = outputs.last_hidden_state[:, 0, :].numpy().flatten()
            
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
    """Spark를 사용한 특허 벡터화 처리 (최적화 버전)"""
    # 최적화: 배치 크기 증가
    if batch_size is None:
        batch_size = 2500 if IS_WINDOWS else 5000
    
    # 디렉토리 설정
    temp_dir = os.path.join(tempfile.gettempdir(), "spark-temp") if IS_WINDOWS else "/tmp/spark-temp"
    os.makedirs(temp_dir, exist_ok=True)
    os.makedirs("models", exist_ok=True)
    
    # 초기 리소스 정리
    clean_temp_files()
    
    # 최적화: CPU 코어 증가
    cpu_cores = min(os.cpu_count() or 4, 8) if IS_WINDOWS else min(os.cpu_count() or 16, 32)
    driver_memory = "12g" if IS_WINDOWS else "110g"
    # 최적화: DB 배치 크기 증가
    db_batch_size = 50 if IS_WINDOWS else 100
    
    # Spark 세션 초기화 (최적화된 설정)
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
            StructField("patent_title", StringType(), True),
            StructField("patent_summary", StringType(), True),
            StructField("patent_claim", StringType(), True),
            StructField("patent_application_number", StringType(), True),
            StructField("patent_ipc", StringType(), True)
        ])
        
        # 벡터 유효성 검사 강화
        for batch_idx in range(last_processed_batch, num_batches):
            await manage_resources()
            
            batch_start = time.time()
            start_idx = batch_idx * batch_size
            end_idx = min((batch_idx + 1) * batch_size, total_patents)
            batch_patents = all_patents[start_idx:end_idx]
            
            logger.info(f"배치 {batch_idx+1}/{num_batches} 처리 중 ({len(batch_patents)}개 특허)")
            
            # 유효 특허 필터링 (병렬 처리 후보)
            valid_patents = []
            for p in batch_patents:
                try:
                    patent_title = p.get("patent_title", "")
                    patent_summary = p.get("patent_summary", "")
                    patent_claim = p.get("patent_claim", "")
                    
                    if any(len(str(field)) > 10 for field in [patent_title, patent_summary, patent_claim]):
                        valid_patents.append(p)
                except Exception as e:
                    logger.error(f"특허 유효성 검사 중 오류: {str(e)}")
            
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
            
            processed = await save_to_database_bulk(patent_rows, with_bert, db_batch_size)  # 최적화된 함수 사용
            total_processed += processed
            
            batch_duration = time.time() - batch_start
            patents_per_second = len(batch_patents) / batch_duration if batch_duration > 0 else 0
            remaining_batches = num_batches - batch_idx - 1
            est_remaining_time = remaining_batches * batch_duration / 60  # 분 단위
            
            logger.info(f"배치 {batch_idx+1}/{num_batches} 완료: {len(batch_patents)}개 처리 ({batch_duration:.2f}초, {patents_per_second:.1f}개/초)")
            logger.info(f"예상 남은 시간: {est_remaining_time:.1f}분 ({remaining_batches}개 배치)")
            
            save_checkpoint(checkpoint_file, batch_idx)
            patent_rows = None
            result_df = None
            df = None
            gc.collect()
            if torch.cuda.is_available():
                torch.cuda.empty_cache()
            
            # 최적화: 대기 시간 단축
            await asyncio.sleep(1)
        
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

# 1. Spark 세션 초기화 함수 (최적화)
def initialize_spark_session(temp_dir, cpu_cores, driver_memory="12g"):
    """Spark 세션을 초기화하고 반환합니다."""
    spark = SparkSession.builder \
        .appName("PatentVectorizer") \
        .config("spark.driver.memory", driver_memory) \
        .config("spark.sql.adaptive.enabled", "true") \
        .config("spark.memory.fraction", "0.8") \
        .config("spark.default.parallelism", str(cpu_cores * 2)) \
        .config("spark.sql.shuffle.partitions", str(cpu_cores * 4)) \
        .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer") \
        .config("spark.local.dir", temp_dir) \
        .master(f"local[{cpu_cores}]") \
        .getOrCreate()
    
    spark.sparkContext.setLogLevel("ERROR")
    return spark

# 2. 체크포인트 관리 함수들 (변경 없음)
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

# 3. 배치 데이터 변환 함수 (변경 없음)
def prepare_batch_data(batch_patents, schema):
    """특허 배치 데이터를 Spark DataFrame으로 변환할 수 있는 형식으로 준비합니다."""
    import re  # 정규식 모듈 추가
    batch_data = []
    for p in batch_patents:
        try:
            # 안전한 키 접근으로 수정
            # 'patent_id' 키가 없으면 'patent_application_number'를 ID로 사용
            patent_id = None
            
            # 'patent_id' 직접 시도
            if 'patent_id' in p:
                patent_id = p['patent_id']
            # 대체 키 시도
            elif 'id' in p:
                patent_id = p['id']
            elif 'patent_application_number' in p:
                # 고유한 ID로 사용하기 위해 해시값 사용 (실제로는 DB에서 ID 생성)
                patent_id = abs(hash(p['patent_application_number'])) % (10 ** 10)
            else:
                # 고유 ID 생성
                patent_id = abs(hash(str(p))) % (10 ** 10)
                logger.warning(f"임시 ID 생성됨: {patent_id}")
            
            # 출원번호 정리 (숫자와 하이픈만 유지)
            patent_application_number = p.get("patent_application_number", "")
            if patent_application_number:
                # 발명의명칭 및 기타 문자 제거, 숫자와 하이픈만 유지
                patent_application_number = re.sub(r'[^0-9\-]', '', patent_application_number)
                
                # 길이 제한 적용
                if len(patent_application_number) > 20:
                    patent_application_number = patent_application_number[:20]
            
            # 필드 안전하게 추출
            batch_data.append((
                patent_id,
                p.get("patent_title", ""),
                p.get("patent_summary", ""),
                p.get("patent_claim", ""),
                patent_application_number,  # 정리된 출원번호 사용
                p.get("patent_ipc", "")
            ))
        except Exception as e:
            logger.error(f"특허 데이터 변환 오류: {str(e)}")
    
    if not batch_data:
        logger.warning("변환된 특허 데이터가 없습니다.")
    else:
        logger.info(f"총 {len(batch_data)}개 특허 데이터 변환 완료")
        
    return batch_data

# 4. 벡터화 함수 (변경 없음)
def vectorize_patents(df, tfidf_vectorize, bert_vectorize=None):
    """특허 DataFrame에 벡터화를 적용합니다."""
    # TF-IDF 벡터화 - 필드명 수정
    result_df = df.withColumn("title_tfidf_vector", tfidf_vectorize(F.col("patent_title")))
    result_df = result_df.withColumn("summary_tfidf_vector", tfidf_vectorize(F.col("patent_summary")))
    result_df = result_df.withColumn("claim_tfidf_vector", tfidf_vectorize(F.col("patent_claim")))
    
    # BERT 벡터화 (선택적) - 필드명 수정
    if bert_vectorize is not None:
        result_df = result_df.withColumn("title_bert_vector", bert_vectorize(F.col("patent_title")))
        result_df = result_df.withColumn("summary_bert_vector", bert_vectorize(F.col("patent_summary")))
        result_df = result_df.withColumn("claim_bert_vector", bert_vectorize(F.col("patent_claim")))
    
    return result_df

# 5. 최적화된 일괄 데이터베이스 저장 함수 (INSERT 사용으로 수정)
async def save_to_database_bulk(patent_rows, with_bert=False, db_batch_size=100):
    """최적화: 벡터화된 특허 데이터의 전체 필드 일괄 삽입 (cluster_id 제외)"""
    total_processed = 0
    insert_values = []
    
    # 데이터 수집
    for row in patent_rows:
        try:
            # 벡터 유효성 검사 확인
            has_vectors = (
                hasattr(row, "title_tfidf_vector") and 
                hasattr(row, "summary_tfidf_vector") and 
                hasattr(row, "claim_tfidf_vector")
            )
            
            valid_vectors = False
            if has_vectors:
                valid_vectors = (
                    row.title_tfidf_vector is not None and
                    row.summary_tfidf_vector is not None and
                    row.claim_tfidf_vector is not None
                )
            
            if valid_vectors and hasattr(row, "patent_id") and row.patent_id:
                now = datetime.utcnow()
                
                # 텍스트 필드 길이 제한 적용
                patent_title = getattr(row, "patent_title", "")
                if not patent_title:
                    patent_title = "제목 없음"
                elif len(patent_title) > 500:
                    patent_title = patent_title[:500]
                    
                patent_summary = getattr(row, "patent_summary", "")
                if not patent_summary:
                    patent_summary = "요약 없음"
                
                patent_claim = getattr(row, "patent_claim", "")
                if not patent_claim:
                    patent_claim = "청구항 없음"
                    
                patent_application_number = getattr(row, "patent_application_number", "")
                if not patent_application_number:
                    patent_application_number = f"APP-{row.patent_id}"
                elif len(patent_application_number) > 20:
                    patent_application_number = patent_application_number[:20]
                    
                patent_ipc = getattr(row, "patent_ipc", "")
                if not patent_ipc:
                    patent_ipc = "미분류"
                elif len(patent_ipc) > 100:
                    patent_ipc = patent_ipc[:100]
                
                insert_values.append({
                    "patent_id": row.patent_id,
                    "patent_title": patent_title,
                    "patent_summary": patent_summary,
                    "patent_claim": patent_claim,
                    "patent_application_number": patent_application_number,
                    "patent_ipc": patent_ipc,
                    "patent_title_tfidf_vector": row.title_tfidf_vector,
                    "patent_summary_tfidf_vector": row.summary_tfidf_vector,
                    "patent_claim_tfidf_vector": row.claim_tfidf_vector,
                    "patent_created_at": now,
                    "patent_updated_at": now
                })
        except Exception as e:
            logger.error(f"데이터 준비 중 오류: {str(e)}")
    
    # 개별 레코드 단위로 삽입
    success_count = 0
    if insert_values:
        for i in range(0, len(insert_values), db_batch_size):
            batch = insert_values[i:i+db_batch_size]
            batch_success = 0
            
            for idx, values in enumerate(batch):
                try:
                    # cluster_id가 제외된 INSERT 쿼리
                    insert_query = """
                    INSERT INTO patent (
                        patent_id, patent_title, patent_summary, patent_claim,
                        patent_application_number, patent_ipc,
                        patent_title_tfidf_vector, patent_summary_tfidf_vector, 
                        patent_claim_tfidf_vector, patent_created_at, patent_updated_at
                    ) VALUES (
                        :patent_id, :patent_title, :patent_summary, :patent_claim,
                        :patent_application_number, :patent_ipc,
                        :patent_title_tfidf_vector, :patent_summary_tfidf_vector,
                        :patent_claim_tfidf_vector, :patent_created_at, :patent_updated_at
                    )
                    """
                    
                    await database.execute(insert_query, values=values)
                    batch_success += 1
                    success_count += 1
                except Exception as e:
                    logger.error(f"레코드 삽입 실패 ({i+idx}/{len(insert_values)}): {str(e)}")
            
            logger.info(f"DB 일괄 삽입: {i+batch_success}/{len(insert_values)} 완료 (배치 성공률: {batch_success}/{len(batch)})")
    
    logger.info(f"총 {success_count}/{len(insert_values)}개 특허 데이터 삽입 완료")
    return success_count

# 기존 save_to_database 함수 유지 (호환성)
async def save_to_database(patent_rows, with_bert=False, db_batch_size=25):
    """기존 데이터베이스 저장 함수 (호환성 유지)"""
    return await save_to_database_bulk(patent_rows, with_bert, db_batch_size)

# 7. 리소스 관리 함수 (변경 없음)
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
