package com.aidflow.campaign.infrastructure.http.dto;

import com.aidflow.campaign.domain.model.Campaign;
import com.aidflow.campaign.domain.model.CampaignPriority;
import com.aidflow.campaign.domain.model.CampaignStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CampaignResponse(
        UUID id,
        String name,
        String description,
        String location,
        CampaignPriority priority,
        CampaignStatus status,
        UUID createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static CampaignResponse from(Campaign campaign) {
        return new CampaignResponse(
                campaign.id(),
                campaign.name(),
                campaign.description(),
                campaign.location(),
                campaign.priority(),
                campaign.status(),
                campaign.createdBy(),
                campaign.createdAt(),
                campaign.updatedAt()
        );
    }
}
