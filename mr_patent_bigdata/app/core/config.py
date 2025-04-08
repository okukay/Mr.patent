from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    # 기존 설정
    DATABASE_URL: str
    PROJECT_NAME: str = "mr_patent_fastapi"
    google_credentials_path: str
    kipris_service_key: str
    
    # ElasticSearch 설정 추가
    elasticsearch_host: str
    elasticsearch_use_ssl: bool = True
    elasticsearch_verify_certs: bool = False
    elasticsearch_username: str
    elasticsearch_password: str
    
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8"
    )

settings = Settings()
