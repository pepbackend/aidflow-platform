import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.config import settings
from app.dlq_publisher import DlqPublisher
from app.event_handler import EventHandler
from app.kafka_consumer import KafkaEventConsumer

logging.basicConfig(level=logging.INFO)

event_consumer = KafkaEventConsumer(
    settings=settings,
    event_handler=EventHandler(),
    dlq_publisher=DlqPublisher(settings),
)


@asynccontextmanager
async def lifespan(app: FastAPI):
    await event_consumer.start()
    yield
    await event_consumer.stop()


app = FastAPI(title="AidFlow Notification Service", lifespan=lifespan)


@app.get("/health")
def health() -> dict[str, str]:
    return {
        "status": "UP",
        "service": settings.service_name,
    }
