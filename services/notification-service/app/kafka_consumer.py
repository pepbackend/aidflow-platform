import asyncio
import logging

from aiokafka import AIOKafkaConsumer
from pydantic import ValidationError

from app.config import Settings
from app.dlq_publisher import DlqPublisher
from app.event_handler import EventHandler
from app.models import EventEnvelope

logger = logging.getLogger(__name__)


class KafkaEventConsumer:
    def __init__(
        self,
        settings: Settings,
        event_handler: EventHandler,
        dlq_publisher: DlqPublisher,
    ) -> None:
        self._settings = settings
        self._event_handler = event_handler
        self._dlq_publisher = dlq_publisher
        self._task: asyncio.Task[None] | None = None
        self._consumer: AIOKafkaConsumer | None = None

    async def start(self) -> None:
        if self._task is None:
            self._task = asyncio.create_task(self._consume_forever())

    async def stop(self) -> None:
        if self._task is not None:
            self._task.cancel()
            try:
                await self._task
            except asyncio.CancelledError:
                pass
            self._task = None

    async def _consume_forever(self) -> None:
        while True:
            try:
                await self._dlq_publisher.start()
                self._consumer = AIOKafkaConsumer(
                    self._settings.kafka_topic,
                    bootstrap_servers=self._settings.kafka_bootstrap_servers,
                    group_id=self._settings.kafka_consumer_group,
                    enable_auto_commit=True,
                    auto_offset_reset="earliest",
                )
                await self._consumer.start()
                logger.info(
                    "Kafka consumer started",
                    extra={
                        "topic": self._settings.kafka_topic,
                        "consumerGroup": self._settings.kafka_consumer_group,
                    },
                )

                async for message in self._consumer:
                    await self.process_message(
                        message.value,
                        topic=message.topic,
                        partition=message.partition,
                        offset=message.offset,
                    )
            except asyncio.CancelledError:
                raise
            except Exception:
                logger.exception(
                    "Kafka consumer loop failed; retrying shortly",
                    extra={
                        "topic": self._settings.kafka_topic,
                        "consumerGroup": self._settings.kafka_consumer_group,
                    },
                )
                await asyncio.sleep(5)
            finally:
                if self._consumer is not None:
                    await self._consumer.stop()
                    self._consumer = None
                await self._dlq_publisher.stop()

    async def process_message(
        self,
        value: bytes | None,
        *,
        topic: str | None = None,
        partition: int | None = None,
        offset: int | None = None,
    ) -> None:
        log_context = {
            "topic": topic,
            "partition": partition,
            "offset": offset,
        }
        try:
            if value is None:
                raise ValueError("Kafka message value is empty")
            raw_event = value.decode("utf-8")
            event = EventEnvelope.model_validate_json(raw_event)
            logger.info(
                "Kafka event received",
                extra=log_context
                | {
                    "eventId": event.event_id,
                    "eventType": event.event_type,
                    "aggregateType": event.aggregate_type,
                    "aggregateId": event.aggregate_id,
                },
            )
            self._event_handler.handle(event)
        except (ValidationError, ValueError, TypeError) as error:
            logger.exception(
                "Event validation failed; publishing to DLQ",
                extra=log_context | {"errorType": error.__class__.__name__},
            )
            await self._publish_to_dlq(self._raw_event(value), error)
        except Exception as error:
            logger.exception(
                "Event handling failed; publishing to DLQ",
                extra=log_context | {"errorType": error.__class__.__name__},
            )
            await self._publish_to_dlq(self._raw_event(value), error)

    async def _publish_to_dlq(self, raw_event: str, error: Exception) -> None:
        try:
            await self._dlq_publisher.publish(raw_event, error)
        except Exception:
            logger.exception("Failed to publish event to DLQ")

    def _raw_event(self, value: bytes | None) -> str:
        if value is None:
            return ""
        return value.decode("utf-8", errors="replace")
