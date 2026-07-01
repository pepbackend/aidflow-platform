from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    service_name: str = "notification-service"
    service_port: int = 8090
    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_topic: str = "aidflow.events"
    kafka_consumer_group: str = "aidflow-notifications"
    kafka_dlq_topic: str = "aidflow.events.dlq"

    model_config = SettingsConfigDict(env_file=".env", extra="ignore")


settings = Settings()
