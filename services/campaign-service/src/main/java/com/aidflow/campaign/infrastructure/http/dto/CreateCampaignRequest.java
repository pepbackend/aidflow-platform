package com.aidflow.campaign.infrastructure.http.dto;

import com.aidflow.campaign.domain.model.CampaignPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCampaignRequest(
        @NotBlank
        @Size(max = 150)
        String name,

        @NotBlank
        String description,

        @NotBlank
        @Size(max = 150)
        String location,

        @NotNull
        CampaignPriority priority
) {
}
