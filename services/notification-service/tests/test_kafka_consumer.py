import asyncio

from app.config import Settings
from app.event_handler import EventHandler
from app.kafka_consumer import KafkaEventConsumer


class FakeDlqPublisher:
    def __init__(self):
        self.published_events = []

    async def publish(self, raw_event: str, error: Exception) -> None:
        self.published_events.append((raw_event, error))


def test_invalid_json_is_published_to_dlq():
    dlq_publisher = FakeDlqPublisher()
    consumer = KafkaEventConsumer(
        settings=Settings(),
        event_handler=EventHandler(),
        dlq_publisher=dlq_publisher,
    )

    asyncio.run(consumer.process_message(b"{not-json"))

    assert len(dlq_publisher.published_events) == 1
    assert dlq_publisher.published_events[0][0] == "{not-json"
