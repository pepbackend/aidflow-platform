import logging
from datetime import UTC, datetime

from aiokafka import AIOKafkaProducer

from app.config import Settings
from app.models import DeadLetterEvent

logger = logging.getLogger(__name__)


class DlqPublisher:
    def __init__(self, settings: Settings) -> None:
        self._settings = settings
        self._producer: AIOKafkaProducer | None = None

    async def start(self) -> None:
        if self._producer is None:
            self._producer = AIOKafkaProducer(
                bootstrap_servers=self._settings.kafka_bootstrap_servers,
            )
            await self._producer.start()

    async def stop(self) -> None:
        if self._producer is not None:
            await self._producer.stop()
            self._producer = None

    async def publish(self, raw_event: str, error: Exception) -> None:
        if self._producer is None:
            raise RuntimeError("DLQ producer has not been started")

        dead_letter = DeadLetterEvent(
            originalEvent=raw_event,
            errorType=error.__class__.__name__,
            errorMessage=str(error),
            failedAt=datetime.now(UTC),
            service=self._settings.service_name,
            consumerGroup=self._settings.kafka_consumer_group,
        )

        await self._producer.send_and_wait(
            self._settings.kafka_dlq_topic,
            dead_letter.model_dump_json(by_alias=True).encode("utf-8"),
        )
        logger.info(
            "Published failed event to DLQ",
            extra={
                "topic": self._settings.kafka_dlq_topic,
                "consumerGroup": self._settings.kafka_consumer_group,
                "errorType": error.__class__.__name__,
            },
        )
