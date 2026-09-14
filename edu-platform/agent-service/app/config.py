import os


class Settings:
    mysql_host = os.getenv("MYSQL_HOST", "localhost")
    mysql_port = int(os.getenv("MYSQL_PORT", "3306"))
    mysql_user = os.getenv("MYSQL_USERNAME", "root")
    mysql_password = os.getenv("MYSQL_PASSWORD", "123456")
    mysql_database = os.getenv("MYSQL_DATABASE", "edu_agent")
    recommendation_url = os.getenv("RECOMMENDATION_BASE_URL", "http://localhost:8085").rstrip("/")
    ai_base_url = os.getenv("AI_BASE_URL", "").rstrip("/")
    ai_api_key = os.getenv("AI_API_KEY", "")
    ai_model = os.getenv("AI_MODEL", "deepseek-chat")


settings = Settings()
