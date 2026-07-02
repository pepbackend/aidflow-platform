package com.aidflow.campaign.infrastructure.outbox;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import net.logstash.logback.argument.StructuredArguments;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxEventPublishingService {
    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublishingService.class);
    private static final String MDC_TRACE_ID_KEY = "traceId";

    private final SpringDataOutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MeterRegistry meterRegistry;
    private final int batchSize;

    public OutboxEventPublishingService(
            SpringDataOutboxEventRepository repository,
            KafkaTemplate<String, String> kafkaTemplate,
            MeterRegistry meterRegistry,
            @Value("${campaign.outbox.batch-size:50}") int batchSize
    ) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.meterRegistry = meterRegistry;
        this.batchSize = batchSize;
    }

    @Transactional
    public void publishPendingEvents() {
        MDC.put(MDC_TRACE_ID_KEY, "outbox-" + UUID.randomUUID());

        List<OutboxEventJpaEntity> pendingEvents = repository.findByStatusOrderByCreatedAtAsc(
                OutboxEventStatus.PENDING,
                PageRequest.of(0, batchSize)
        );

        int publishedCount = 0;
        int failedCount = 0;

        try {
            log.info(
                    "Outbox publish batch started",
                    StructuredArguments.keyValue("batchSize", batchSize),
                    StructuredArguments.keyValue("pendingCount", pendingEvents.size())
            );

            for (OutboxEventJpaEntity event : pendingEvents) {
                String key = event.getAggregateId().toString();
                try {
                    SendResult<String, String> result = kafkaTemplate
                            .send(event.getTopic(), key, event.getPayload())
                            .get();
                    RecordMetadata metadata = result.getRecordMetadata();
                    event.markPublished(OffsetDateTime.now(ZoneOffset.UTC));
                    publishedCount++;
                    incrementKafkaEventCounter("aidflow.kafka.events.published", event, "success");
                    log.info(
                            "Kafka event published",
                            StructuredArguments.keyValue("eventId", event.getId()),
                            StructuredArguments.keyValue("aggregateType", event.getAggregateType()),
                            StructuredArguments.keyValue("aggregateId", event.getAggregateId()),
                            StructuredArguments.keyValue("eventType", event.getEventType()),
                            StructuredArguments.keyValue("topic", metadata.topic()),
                            StructuredArguments.keyValue("partition", metadata.partition()),
                            StructuredArguments.keyValue("offset", metadata.offset()),
                            StructuredArguments.keyValue("key", key)
                    );
                } catch (Exception exception) {
                    event.markFailed(exception.getMessage());
                    failedCount++;
                    incrementKafkaEventCounter("aidflow.kafka.events.publication.failed", event, "failed");
                    log.error(
                            "Kafka event publication failed",
                            StructuredArguments.keyValue("eventId", event.getId()),
                            StructuredArguments.keyValue("aggregateType", event.getAggregateType()),
                            StructuredArguments.keyValue("aggregateId", event.getAggregateId()),
                            StructuredArguments.keyValue("eventType", event.getEventType()),
                            StructuredArguments.keyValue("topic", event.getTopic()),
                            StructuredArguments.keyValue("key", key),
                            StructuredArguments.keyValue("errorType", exception.getClass().getSimpleName()),
                            StructuredArguments.keyValue("errorMessage", exception.getMessage()),
                            exception
                    );
                }
            }

            log.info(
                    "Outbox publish batch finished",
                    StructuredArguments.keyValue("pendingCount", pendingEvents.size()),
                    StructuredArguments.keyValue("publishedCount", publishedCount),
                    StructuredArguments.keyValue("failedCount", failedCount)
            );
        } finally {
            MDC.remove(MDC_TRACE_ID_KEY);
        }
    }

    private void incrementKafkaEventCounter(String metricName, OutboxEventJpaEntity event, String status) {
        Counter.builder(metricName)
                .description("Application-level Kafka event publication count.")
                .tag("service", "campaign-service")
                .tag("event_type", event.getEventType())
                .tag("aggregate_type", event.getAggregateType())
                .tag("topic", event.getTopic())
                .tag("status", status)
                .register(meterRegistry)
                .increment();
    }
}
