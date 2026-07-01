package com.aidflow.campaign.infrastructure.outbox;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import net.logstash.logback.argument.StructuredArguments;
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
    private final int batchSize;

    public OutboxEventPublishingService(
            SpringDataOutboxEventRepository repository,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${campaign.outbox.batch-size:50}") int batchSize
    ) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
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
                try {
                    SendResult<String, String> result = kafkaTemplate
                            .send(event.getTopic(), event.getAggregateId().toString(), event.getPayload())
                            .get();
                    event.markPublished(OffsetDateTime.now(ZoneOffset.UTC));
                    publishedCount++;
                    log.info(
                            "Outbox event published",
                            StructuredArguments.keyValue("outboxEventId", event.getId()),
                            StructuredArguments.keyValue("aggregateType", event.getAggregateType()),
                            StructuredArguments.keyValue("aggregateId", event.getAggregateId()),
                            StructuredArguments.keyValue("eventType", event.getEventType()),
                            StructuredArguments.keyValue("topic", result.getRecordMetadata().topic()),
                            StructuredArguments.keyValue("partition", result.getRecordMetadata().partition()),
                            StructuredArguments.keyValue("offset", result.getRecordMetadata().offset())
                    );
                } catch (Exception exception) {
                    event.markFailed(exception.getMessage());
                    failedCount++;
                    log.warn(
                            "Outbox event publish failed",
                            StructuredArguments.keyValue("outboxEventId", event.getId()),
                            StructuredArguments.keyValue("aggregateType", event.getAggregateType()),
                            StructuredArguments.keyValue("aggregateId", event.getAggregateId()),
                            StructuredArguments.keyValue("eventType", event.getEventType()),
                            StructuredArguments.keyValue("topic", event.getTopic()),
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
}
