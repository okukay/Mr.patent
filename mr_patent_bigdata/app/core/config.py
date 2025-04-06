from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    DATABASE_URL: str = "mysql+pymysql://mr_patent:ssafy@j12d208.p.ssafy.io:3306/mr_patent"
    PROJECT_NAME: str = "mr_patent_fastapi"
    google_credentials_path: str

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8"
    )

settings = Settings()
