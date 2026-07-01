from contextlib import asynccontextmanager

from fastapi import FastAPI
from starlette.requests import Request

from app.config import settings
from app.dlq_publisher import DlqPublisher
from app.event_handler import EventHandler
from app.kafka_consumer import KafkaEventConsumer
from app.tracing import TRACE_ID_HEADER, configure_logging, new_trace_id, trace_id_context

configure_logging()

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


@app.middleware("http")
async def trace_id_middleware(request: Request, call_next):
    trace_id = request.headers.get(TRACE_ID_HEADER) or new_trace_id()
    token = trace_id_context.set(trace_id)

    try:
        response = await call_next(request)
        response.headers[TRACE_ID_HEADER] = trace_id
        return response
    finally:
        trace_id_context.reset(token)


@app.get("/health")
def health() -> dict[str, str]:
    return {
        "status": "UP",
        "service": settings.service_name,
    }
