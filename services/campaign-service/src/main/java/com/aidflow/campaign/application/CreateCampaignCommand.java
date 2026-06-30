package com.aidflow.campaign.application;

import com.aidflow.campaign.domain.model.CampaignPriority;

public record CreateCampaignCommand(
        String name,
        String description,
        String location,
        CampaignPriority priority,
        AuthenticatedUser user
) {
}
