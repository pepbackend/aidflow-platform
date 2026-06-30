package com.aidflow.campaign.infrastructure.outbox;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxEventPublisher {
    private final SpringDataOutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final boolean scheduledPublishingEnabled;
    private final int batchSize;

    public OutboxEventPublisher(
            SpringDataOutboxEventRepository repository,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${campaign.outbox.publisher.enabled:true}") boolean scheduledPublishingEnabled,
            @Value("${campaign.outbox.batch-size:50}") int batchSize
    ) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.scheduledPublishingEnabled = scheduledPublishingEnabled;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${campaign.outbox.publisher-fixed-delay:5000}")
    public void publishPendingEventsOnSchedule() {
        if (scheduledPublishingEnabled) {
            publishPendingEvents();
        }
    }

    @Transactional
    public void publishPendingEvents() {
        List<OutboxEventJpaEntity> pendingEvents = repository.findByStatusOrderByCreatedAtAsc(
                OutboxEventStatus.PENDING,
                PageRequest.of(0, batchSize)
        );

        for (OutboxEventJpaEntity event : pendingEvents) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getAggregateId().toString(), event.getPayload()).get();
                event.markPublished(OffsetDateTime.now(ZoneOffset.UTC));
            } catch (Exception exception) {
                event.markFailed(exception.getMessage());
            }
        }
    }
}
