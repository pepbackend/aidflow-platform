package com.aidflow.campaign.application;

import com.aidflow.campaign.domain.model.Campaign;
import com.aidflow.campaign.domain.model.CampaignPriority;
import com.aidflow.campaign.domain.model.CampaignStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CampaignCreatedEvent(
        UUID eventId,
        String eventType,
        UUID campaignId,
        String name,
        String description,
        String location,
        CampaignPriority priority,
        CampaignStatus status,
        UUID createdBy,
        OffsetDateTime createdAt
) {
    public static CampaignCreatedEvent from(Campaign campaign) {
        return new CampaignCreatedEvent(
                UUID.randomUUID(),
                "CampaignCreated",
                campaign.id(),
                campaign.name(),
                campaign.description(),
                campaign.location(),
                campaign.priority(),
                campaign.status(),
                campaign.createdBy(),
                campaign.createdAt()
        );
    }
}
