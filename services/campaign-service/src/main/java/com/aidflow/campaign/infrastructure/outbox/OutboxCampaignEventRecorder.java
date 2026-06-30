package com.aidflow.campaign.infrastructure.outbox;

import com.aidflow.campaign.application.CampaignCreatedEvent;
import com.aidflow.campaign.application.CampaignEventRecorder;
import com.aidflow.campaign.domain.model.Campaign;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OutboxCampaignEventRecorder implements CampaignEventRecorder {
    private final SpringDataOutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    private final String campaignCreatedTopic;

    public OutboxCampaignEventRecorder(
            SpringDataOutboxEventRepository repository,
            ObjectMapper objectMapper,
            @Value("${campaign.events.campaign-created-topic:campaign-created}") String campaignCreatedTopic
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.campaignCreatedTopic = campaignCreatedTopic;
    }

    @Override
    public void recordCampaignCreated(Campaign campaign) {
        CampaignCreatedEvent event = CampaignCreatedEvent.from(campaign);
        repository.save(OutboxEventJpaEntity.pending(
                event.eventId(),
                "Campaign",
                campaign.id(),
                event.eventType(),
                campaignCreatedTopic,
                serialize(event),
                OffsetDateTime.now()
        ));
    }

    private String serialize(CampaignCreatedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize campaign created event", exception);
        }
    }
}
