package com.aidflow.campaign.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Campaign(
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
}
