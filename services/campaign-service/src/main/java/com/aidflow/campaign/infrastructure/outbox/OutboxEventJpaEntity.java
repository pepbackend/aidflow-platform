package com.aidflow.campaign.infrastructure.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEventJpaEntity {
    @Id
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, length = 150)
    private String topic;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OutboxEventStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    protected OutboxEventJpaEntity() {
    }

    private OutboxEventJpaEntity(
            UUID id,
            String aggregateType,
            UUID aggregateId,
            String eventType,
            String topic,
            String payload,
            OutboxEventStatus status,
            OffsetDateTime createdAt
    ) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.topic = topic;
        this.payload = payload;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static OutboxEventJpaEntity pending(
            UUID id,
            String aggregateType,
            UUID aggregateId,
            String eventType,
            String topic,
            String payload,
            OffsetDateTime createdAt
    ) {
        return new OutboxEventJpaEntity(
                id,
                aggregateType,
                aggregateId,
                eventType,
                topic,
                payload,
                OutboxEventStatus.PENDING,
                createdAt
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getTopic() {
        return topic;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxEventStatus getStatus() {
        return status;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void markPublished(OffsetDateTime publishedAt) {
        this.status = OutboxEventStatus.PUBLISHED;
        this.publishedAt = publishedAt;
        this.failureReason = null;
    }

    public void markFailed(String failureReason) {
        this.status = OutboxEventStatus.FAILED;
        this.failureReason = failureReason;
    }
}
