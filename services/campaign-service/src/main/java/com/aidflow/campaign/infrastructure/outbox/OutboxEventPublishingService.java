package com.aidflow.campaign.infrastructure.outbox;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxEventPublishingService {
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
