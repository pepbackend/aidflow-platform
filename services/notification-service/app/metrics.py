from prometheus_client import Counter

KAFKA_EVENTS_RECEIVED = Counter(
    "aidflow_notification_kafka_events_received_total",
    "Kafka events received by the notification service.",
    ["event_type", "aggregate_type"],
)

KAFKA_EVENTS_FAILED = Counter(
    "aidflow_notification_kafka_events_failed_total",
    "Kafka events that failed validation or handling.",
    ["reason"],
)

DLQ_EVENTS_PUBLISHED = Counter(
    "aidflow_notification_dlq_events_published_total",
    "Failed events published to the notification service dead-letter topic.",
    ["error_type"],
)
