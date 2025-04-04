from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    DATABASE_URL: str = "mysql+pymysql://root:0000@localhost/mr_patent"
    PROJECT_NAME: str = "특허 처리 API"
    google_credentials_path: str  # Google 인증 정보 경로 필드 추가
    
    # V2에서는 Config 대신 model_config 사용
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8"
    )

settings = Settings()
