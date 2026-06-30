package com.aidflow.campaign.infrastructure.outbox;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxEventPublisher {
    private final OutboxEventPublishingService publishingService;
    private final boolean scheduledPublishingEnabled;

    public OutboxEventPublisher(
            OutboxEventPublishingService publishingService,
            @Value("${campaign.outbox.publisher.enabled:true}") boolean scheduledPublishingEnabled
    ) {
        this.publishingService = publishingService;
        this.scheduledPublishingEnabled = scheduledPublishingEnabled;
    }

    @Scheduled(fixedDelayString = "${campaign.outbox.publisher-fixed-delay:5000}")
    public void publishPendingEventsOnSchedule() {
        if (scheduledPublishingEnabled) {
            publishPendingEvents();
        }
    }

    public void publishPendingEvents() {
        publishingService.publishPendingEvents();
    }
}
