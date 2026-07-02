from prometheus_client import Counter, Histogram

KAFKA_EVENTS_CONSUMED = Counter(
    "aidflow_kafka_events_consumed_total",
    "Kafka events consumed by an AidFlow application service.",
    ["service", "event_type", "aggregate_type", "topic", "consumer_group", "status"],
)

KAFKA_EVENTS_PROCESSED = Counter(
    "aidflow_kafka_events_processed_total",
    "Kafka events processed by an AidFlow application service.",
    ["service", "event_type", "aggregate_type", "topic", "consumer_group", "status"],
)

KAFKA_EVENTS_PROCESSING_FAILED = Counter(
    "aidflow_kafka_events_processing_failed_total",
    "Kafka events that failed validation or handling in an AidFlow application service.",
    ["service", "event_type", "aggregate_type", "topic", "consumer_group", "status"],
)

KAFKA_EVENT_PROCESSING_DURATION = Histogram(
    "aidflow_kafka_event_processing_duration_seconds",
    "Time spent processing Kafka events in an AidFlow application service.",
    ["service", "event_type", "aggregate_type", "topic", "consumer_group", "handler"],
)

NOTIFICATIONS_SENT = Counter(
    "aidflow_notifications_sent_total",
    "Notifications sent by the notification service.",
    ["service", "event_type", "aggregate_type", "topic", "consumer_group", "status"],
)

DLQ_EVENTS_PUBLISHED = Counter(
    "aidflow_notification_dlq_events_published_total",
    "Failed events published to the notification service dead-letter topic.",
    ["error_type"],
)
