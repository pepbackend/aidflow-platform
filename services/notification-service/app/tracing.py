import logging
from contextvars import ContextVar
from uuid import uuid4

TRACE_ID_HEADER = "X-Trace-Id"

trace_id_context: ContextVar[str] = ContextVar("trace_id", default="-")


class TraceIdLogFilter(logging.Filter):
    def filter(self, record: logging.LogRecord) -> bool:
        record.trace_id = trace_id_context.get()
        return True


def configure_logging() -> None:
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)-5s [notification-service,traceId=%(trace_id)s] [%(threadName)s] %(name)s - %(message)s",
        force=True,
    )

    trace_filter = TraceIdLogFilter()
    root_logger = logging.getLogger()
    for handler in root_logger.handlers:
        handler.addFilter(trace_filter)


def new_trace_id() -> str:
    return str(uuid4())
