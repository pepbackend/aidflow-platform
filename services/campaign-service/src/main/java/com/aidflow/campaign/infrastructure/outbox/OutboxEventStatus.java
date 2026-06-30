package com.aidflow.campaign.infrastructure.outbox;

public enum OutboxEventStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
