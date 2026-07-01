import json
import logging
from datetime import UTC, datetime
from contextvars import ContextVar
from uuid import uuid4

TRACE_ID_HEADER = "X-Trace-Id"

trace_id_context: ContextVar[str] = ContextVar("trace_id", default="-")

_RESERVED_LOG_RECORD_FIELDS = set(logging.makeLogRecord({}).__dict__) | {
    "asctime",
    "message",
    "trace_id",
}


class JsonLogFormatter(logging.Formatter):
    def format(self, record: logging.LogRecord) -> str:
        log_event = {
            "timestamp": datetime.fromtimestamp(record.created, UTC).isoformat(),
            "severity": record.levelname,
            "service": "notification-service",
            "traceId": getattr(record, "trace_id", trace_id_context.get()),
            "logger": record.name,
            "thread": record.threadName,
            "message": record.getMessage(),
        }

        for key, value in record.__dict__.items():
            if key not in _RESERVED_LOG_RECORD_FIELDS and not key.startswith("_"):
                log_event[key] = value

        if record.exc_info:
            log_event["exception"] = self.formatException(record.exc_info)

        return json.dumps(log_event, default=str, separators=(",", ":"))


class TraceIdLogFilter(logging.Filter):
    def filter(self, record: logging.LogRecord) -> bool:
        record.trace_id = trace_id_context.get()
        return True


def configure_logging() -> None:
    logging.basicConfig(
        level=logging.INFO,
        force=True,
    )

    trace_filter = TraceIdLogFilter()
    root_logger = logging.getLogger()
    for handler in root_logger.handlers:
        handler.addFilter(trace_filter)
        handler.setFormatter(JsonLogFormatter())


def new_trace_id() -> str:
    return str(uuid4())
