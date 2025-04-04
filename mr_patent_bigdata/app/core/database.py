import databases
from sqlalchemy import create_engine, MetaData, Table, Column, Integer, String, LargeBinary, Text, TIMESTAMP, Date, Float, JSON, ForeignKey
from sqlalchemy.dialects.mysql import DECIMAL, TINYINT
from datetime import datetime

from app.core.config import settings

# 데이터베이스 연결
database = databases.Database(settings.DATABASE_URL)
metadata = MetaData()

# 사용자 테이블
user = Table(
    "user",
    metadata,
    Column("user_id", Integer, primary_key=True, autoincrement=True),
    Column("user_email", String(50), nullable=False, unique=True),
    Column("user_pw", String(255), nullable=False),
    Column("user_nickname", String(25), nullable=False, unique=True),
    Column("user_image", String(255), nullable=True),
    Column("user_role", TINYINT, nullable=False, default=0),
    Column("user_refresh_token", String(300), nullable=True),
    Column("user_created_at", TIMESTAMP, nullable=False, default=datetime.utcnow),
    Column("user_updated_at", TIMESTAMP, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)
)

# 변리사 테이블
expert = Table(
    "expert",
    metadata,
    Column("expert_id", Integer, primary_key=True, autoincrement=True),
    Column("user_id", Integer, ForeignKey("user.user_id"), nullable=False),
    Column("expert_name", String(25), nullable=False),
    Column("expert_identification", String(15), nullable=False, default="0"),
    Column("expert_description", String(500), nullable=True),
    Column("expert_address", String(300), nullable=True),
    Column("expert_phone", String(15), nullable=True),
    Column("expert_est_date", Date, nullable=True),
    Column("expert_license", String(255), nullable=True),
    Column("expert_license_number", String(30), nullable=True),
    Column("expert_status", TINYINT, nullable=False, default=0),
    Column("expert_created_at", TIMESTAMP, nullable=False, default=datetime.utcnow),
    Column("expert_updated_at", TIMESTAMP, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow),
    Column("expert_category", String(50), nullable=True)
)

# 회원 특허 폴더 테이블
user_patent_folder = Table(
    "user_patent_folder",
    metadata,
    Column("user_patent_folder_id", Integer, primary_key=True, autoincrement=True),
    Column("user_id", Integer, ForeignKey("user.user_id"), nullable=False),
    Column("user_patent_folder_title", String(100), nullable=False),
    Column("user_patent_folder_created_at", TIMESTAMP, nullable=False, default=datetime.utcnow),
    Column("user_patent_folder_updated_at", TIMESTAMP, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)
)

# 특허 테이블
patent = Table(
    "patent",
    metadata,
    Column("patent_id", Integer, primary_key=True, autoincrement=True),
    Column("patent_title", String(100), nullable=False),
    Column("patent_application_number", String(20), nullable=False),
    Column("patent_ipc", String(100), nullable=False),
    Column("patent_summary", Text(16777215), nullable=False),
    Column("patent_claim", Text(16777215), nullable=False),
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
    Column("patent_draft_id", Integer, primary_key=True, autoincrement=True),
    Column("user_patent_folder_id", Integer, ForeignKey("user_patent_folder.user_patent_folder_id"), nullable=False),
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
    Column("fitness_id", Integer, primary_key=True, autoincrement=True),
    Column("patent_draft_id", Integer, ForeignKey("patent_draft.patent_draft_id"), nullable=False),
    Column("fitness_good_content", JSON, nullable=False),
    Column("fitness_is_corrected", TINYINT, nullable=False, default=0),
    Column("fitness_created_at", TIMESTAMP, nullable=False, default=datetime.utcnow),
    Column("fitness_updated_at", TIMESTAMP, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)
)

# 특허 공고전문 테이블
patent_public = Table(
    "patent_public",
    metadata,
    Column("patent_public_id", Integer, primary_key=True, autoincrement=True),
    Column("patent_id", Integer, ForeignKey("patent.patent_id"), nullable=False),
    Column("patent_public_number", String(20), nullable=False),
    Column("patent_public_pdf_path", String(255), nullable=False),
    Column("patent_public_pdf_name", String(100), nullable=False),
    Column("patent_public_content", Text, nullable=False),
    Column("patent_public_api_response", Text, nullable=False),
    Column("patent_public_is_processed", TINYINT, nullable=False, default=0),
    Column("patent_public_retrieved_at", TIMESTAMP, nullable=False, default=datetime.utcnow),
    Column("patent_public_created_at", TIMESTAMP, nullable=False, default=datetime.utcnow),
    Column("patent_public_updated_at", TIMESTAMP, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)
)

# 유사도 테이블
similarity = Table(
    "similarity",
    metadata,
    Column("similarity_id", Integer, primary_key=True, autoincrement=True),
    Column("patent_draft_id", Integer, ForeignKey("patent_draft.patent_draft_id"), nullable=False),
    Column("similarity_created_at", TIMESTAMP, nullable=False, default=datetime.utcnow),
    Column("similarity_updated_at", TIMESTAMP, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)
)

# 유사 특허 테이블
similarity_patent = Table(
    "similarity_patent",
    metadata,
    Column("similarity_patent_id", Integer, primary_key=True, autoincrement=True),
    Column("patent_id", Integer, ForeignKey("patent.patent_id"), nullable=False),
    Column("similarity_id", Integer, ForeignKey("similarity.similarity_id"), nullable=False),
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
    Column("detailed_comparison_id", Integer, primary_key=True, autoincrement=True),
    Column("patent_draft_id", Integer, ForeignKey("patent_draft.patent_draft_id"), nullable=False),
    Column("patent_public_id", Integer, ForeignKey("patent_public.patent_public_id"), nullable=False),
    Column("similarity_patent_id", Integer, ForeignKey("similarity_patent.similarity_patent_id"), nullable=False),
    Column("detailed_comparison_result", Text, nullable=False),
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
    Column("status", String(20)),
    Column("progress", Integer),
    Column("total_files", Integer),
    Column("processed_files", Integer),
    Column("created_at", TIMESTAMP, default=datetime.utcnow),
    Column("updated_at", TIMESTAMP, default=datetime.utcnow, onupdate=datetime.utcnow)
)
