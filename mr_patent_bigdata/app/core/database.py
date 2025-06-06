import databases
from sqlalchemy import create_engine, MetaData, Table, Column, BigInteger, Integer, String, LargeBinary, Text, TIMESTAMP, Float, JSON, ForeignKey
from sqlalchemy.dialects.mysql import TINYINT, MEDIUMTEXT
from datetime import datetime

from app.core.config import settings

# 데이터베이스 연결
database = databases.Database(settings.DATABASE_URL)
metadata = MetaData()

# 회원 특허 폴더 테이블
user_patent_folder = Table(
    "user_patent_folder",
    metadata,
    Column("user_patent_folder_id", BigInteger, primary_key=True, autoincrement=True),
    Column("user_id", BigInteger, nullable=False),  # 외래 키 제약 없이 정의
    Column("user_patent_folder_title", String(100), nullable=False),
    Column("user_patent_folder_created_at", TIMESTAMP, nullable=False, default=datetime.utcnow),
    Column("user_patent_folder_updated_at", TIMESTAMP, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)
)

# 특허 테이블
patent = Table(
    "patent",
    metadata,
    Column("patent_id", BigInteger, primary_key=True, autoincrement=True),
    Column("patent_title", String(500), nullable=False),
    Column("patent_application_number", String(20), nullable=False),
    Column("patent_ipc", String(100), nullable=False),
    Column("patent_summary", MEDIUMTEXT, nullable=False),
    Column("patent_claim", MEDIUMTEXT, nullable=False),
    Column("patent_title_tfidf_vector", LargeBinary, nullable=False),
    Column("patent_summary_tfidf_vector", LargeBinary, nullable=False),
    Column("patent_claim_tfidf_vector", LargeBinary, nullable=False),
    Column("patent_created_at", TIMESTAMP, nullable=False, default=datetime.utcnow),
    Column("patent_updated_at", TIMESTAMP, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)
)

# 특허 초안 테이블
patent_draft = Table(
    "patent_draft",
    metadata,
    Column("patent_draft_id", BigInteger, primary_key=True, autoincrement=True),
    Column("user_patent_folder_id", BigInteger, ForeignKey("user_patent_folder.user_patent_folder_id"), nullable=False),
    Column("patent_draft_title", String(50), nullable=False),
    Column("patent_draft_technical_field", Text, nullable=False),
    Column("patent_draft_background", Text, nullable=False),
    Column("patent_draft_problem", Text, nullable=False),
    Column("patent_draft_solution", Text, nullable=False),
    Column("patent_draft_effect", Text, nullable=False),
    Column("patent_draft_detailed", Text, nullable=False),
    Column("patent_draft_summary", Text, nullable=False),
    Column("patent_draft_claim", Text, nullable=False),
    Column("patent_draft_title_tfidf_vector", LargeBinary, nullable=False),
    Column("patent_draft_title_bert_vector", LargeBinary, nullable=False),
    Column("patent_draft_technical_field_tfidf_vector", LargeBinary, nullable=False),
    Column("patent_draft_technical_field_bert_vector", LargeBinary, nullable=False),
    Column("patent_draft_background_tfidf_vector", LargeBinary, nullable=False),
    Column("patent_draft_background_bert_vector", LargeBinary, nullable=False),
    Column("patent_draft_problem_tfidf_vector", LargeBinary, nullable=False),
    Column("patent_draft_problem_bert_vector", LargeBinary, nullable=False),
    Column("patent_draft_solution_tfidf_vector", LargeBinary, nullable=False),
    Column("patent_draft_solution_bert_vector", LargeBinary, nullable=False),
    Column("patent_draft_effect_tfidf_vector", LargeBinary, nullable=False),
    Column("patent_draft_effect_bert_vector", LargeBinary, nullable=False),
    Column("patent_draft_detailed_tfidf_vector", LargeBinary, nullable=False),
    Column("patent_draft_detailed_bert_vector", LargeBinary, nullable=False),
    Column("patent_draft_summary_tfidf_vector", LargeBinary, nullable=False),
    Column("patent_draft_summary_bert_vector", LargeBinary, nullable=False),
    Column("patent_draft_claim_tfidf_vector", LargeBinary, nullable=False),
    Column("patent_draft_claim_bert_vector", LargeBinary, nullable=False),
    Column("patent_draft_created_at", TIMESTAMP, nullable=False, default=datetime.utcnow),
    Column("patent_draft_updated_at", TIMESTAMP, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)
)

# 적합도 테이블
fitness = Table(
    "fitness",
    metadata,
    Column("fitness_id", BigInteger, primary_key=True, autoincrement=True),
    Column("patent_draft_id", BigInteger, ForeignKey("patent_draft.patent_draft_id"), nullable=False),
    Column("fitness_good_content", JSON, nullable=False),
    Column("fitness_is_corrected", TINYINT, nullable=False, default=0),
    Column("fitness_created_at", TIMESTAMP, nullable=False, default=datetime.utcnow),
    Column("fitness_updated_at", TIMESTAMP, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)
)

# 특허 공고전문 테이블
patent_public = Table(
    "patent_public",
    metadata,
    Column("patent_public_id", BigInteger, primary_key=True, autoincrement=True),
    Column("patent_id", BigInteger, ForeignKey("patent.patent_id"), nullable=False),
    Column("patent_public_number", String(20), nullable=False),
    Column("patent_public_content", MEDIUMTEXT, nullable=False),
    Column("patent_public_api_response", MEDIUMTEXT, nullable=False),
    Column("patent_public_created_at", TIMESTAMP, nullable=False, default=datetime.utcnow),
    Column("patent_public_updated_at", TIMESTAMP, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)
)

# 유사도 테이블
similarity = Table(
    "similarity",
    metadata,
    Column("similarity_id", BigInteger, primary_key=True, autoincrement=True),
    Column("patent_draft_id", BigInteger, ForeignKey("patent_draft.patent_draft_id"), nullable=False),
    Column("similarity_created_at", TIMESTAMP, nullable=False, default=datetime.utcnow),
    Column("similarity_updated_at", TIMESTAMP, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)
)

# 유사 특허 테이블
similarity_patent = Table(
    "similarity_patent",
    metadata,
    Column("similarity_patent_id", BigInteger, primary_key=True, autoincrement=True),
    Column("patent_id", BigInteger, ForeignKey("patent.patent_id"), nullable=False),
    Column("similarity_id", BigInteger, ForeignKey("similarity.similarity_id"), nullable=False),
    Column("similarity_patent_score", Float, nullable=False),
    Column("similarity_patent_claim", Float, nullable=False),
    Column("similarity_patent_summary", Float, nullable=False),
    Column("similarity_patent_title", Float, nullable=False),
    Column("similarity_patent_created_at", TIMESTAMP, nullable=False, default=datetime.utcnow),
    Column("similarity_patent_updated_at", TIMESTAMP, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)
)

# 상세 비교 테이블
detailed_comparison = Table(
    "detailed_comparison",
    metadata,
    Column("detailed_comparison_id", BigInteger, primary_key=True, autoincrement=True),
    Column("patent_draft_id", BigInteger, ForeignKey("patent_draft.patent_draft_id"), nullable=False),
    Column("similarity_patent_id", BigInteger, ForeignKey("similarity_patent.similarity_patent_id"), nullable=False),
    Column("patent_public_id", BigInteger, ForeignKey("patent_public.patent_public_id"), nullable=False),
    Column("detailed_comparison_result", JSON, nullable=False),
    Column("detailed_comparison_context", JSON, nullable=False),
    Column("detailed_comparison_total_score", Float, nullable=False),
    Column("detailed_comparison_created_at", TIMESTAMP, nullable=False, default=datetime.utcnow),
    Column("detailed_comparison_updated_at", TIMESTAMP, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)
)

# 작업 상태 테이블
task_status = Table(
    "task_status",
    metadata,
    Column("task_id", String(50), primary_key=True),
    Column("patent_draft_id", BigInteger, ForeignKey("patent_draft.patent_draft_id"), nullable=False),
    Column("task_type", String(50), nullable=False),
    Column("task_status", String(50), nullable=False),
    Column("task_error_message", Text),
    Column("task_created_at", TIMESTAMP, nullable=False, default=datetime.utcnow),
    Column("task_updated_at", TIMESTAMP, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)
)

