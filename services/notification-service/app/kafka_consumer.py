import asyncio
import logging
import time

from aiokafka import AIOKafkaConsumer
from pydantic import ValidationError

from app.config import Settings
from app.dlq_publisher import DlqPublisher
from app.event_handler import EventHandler
from app.metrics import (
    KAFKA_EVENT_PROCESSING_DURATION,
    KAFKA_EVENTS_CONSUMED,
    KAFKA_EVENTS_PROCESSED,
    KAFKA_EVENTS_PROCESSING_FAILED,
    NOTIFICATIONS_SENT,
)
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
            "consumerGroup": self._settings.kafka_consumer_group,
        }
        event: EventEnvelope | None = None
        try:
            if value is None:
                raise ValueError("Kafka message value is empty")
            raw_event = value.decode("utf-8")
            event = EventEnvelope.model_validate_json(raw_event)
            self._increment_event_counter(KAFKA_EVENTS_CONSUMED, event, topic, "success")
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
            started_at = time.perf_counter()
            result = self._event_handler.handle(event)
            KAFKA_EVENT_PROCESSING_DURATION.labels(
                service=self._settings.service_name,
                event_type=event.event_type,
                aggregate_type=event.aggregate_type,
                topic=topic or self._settings.kafka_topic,
                consumer_group=self._settings.kafka_consumer_group,
                handler=result.handler,
            ).observe(time.perf_counter() - started_at)
            self._increment_event_counter(KAFKA_EVENTS_PROCESSED, event, topic, "success")
            if result.notification_sent:
                self._increment_event_counter(NOTIFICATIONS_SENT, event, topic, "success")
            logger.info(
                "Kafka event processed",
                extra=log_context
                | {
                    "eventId": event.event_id,
                    "eventType": event.event_type,
                    "aggregateType": event.aggregate_type,
                    "aggregateId": event.aggregate_id,
                    "handler": result.handler,
                    "processingResult": result.processing_result,
                },
            )
        except (ValidationError, ValueError, TypeError) as error:
            logger.exception(
                "Kafka event processing failed",
                extra=self._failure_log_context(log_context, event, error),
            )
            self._increment_event_counter(KAFKA_EVENTS_PROCESSING_FAILED, event, topic, "failed")
            await self._publish_to_dlq(self._raw_event(value), error)
        except Exception as error:
            logger.exception(
                "Kafka event processing failed",
                extra=self._failure_log_context(log_context, event, error),
            )
            self._increment_event_counter(KAFKA_EVENTS_PROCESSING_FAILED, event, topic, "failed")
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

    def _increment_event_counter(
        self,
        counter,
        event: EventEnvelope | None,
        topic: str | None,
        status: str,
    ) -> None:
        counter.labels(
            service=self._settings.service_name,
            event_type=event.event_type if event else "unknown",
            aggregate_type=event.aggregate_type if event else "unknown",
            topic=topic or self._settings.kafka_topic,
            consumer_group=self._settings.kafka_consumer_group,
            status=status,
        ).inc()

    def _failure_log_context(
        self,
        log_context: dict[str, object],
        event: EventEnvelope | None,
        error: Exception,
    ) -> dict[str, object]:
        event_context = {}
        if event is not None:
            event_context = {
                "eventId": event.event_id,
                "eventType": event.event_type,
                "aggregateType": event.aggregate_type,
                "aggregateId": event.aggregate_id,
            }
        return log_context | event_context | {
            "errorType": error.__class__.__name__,
            "errorMessage": str(error),
        }
