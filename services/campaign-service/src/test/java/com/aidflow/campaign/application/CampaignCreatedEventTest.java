package com.aidflow.campaign.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.aidflow.campaign.domain.model.Campaign;
import com.aidflow.campaign.domain.model.CampaignPriority;
import com.aidflow.campaign.domain.model.CampaignStatus;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CampaignCreatedEventTest {
    @Test
    void createsEventPayloadFromCampaign() {
        UUID campaignId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.of(2026, 6, 30, 10, 15, 30, 0, ZoneOffset.UTC);
        Campaign campaign = new Campaign(
                campaignId,
                "Flood response in Granollers",
                "Volunteer coordination after heavy rain flooding",
                "Granollers",
                CampaignPriority.HIGH,
                CampaignStatus.ACTIVE,
                createdBy,
                createdAt,
                createdAt
        );

        CampaignCreatedEvent event = CampaignCreatedEvent.from(campaign);

        assertThat(event.eventId()).isNotNull();
        assertThat(event.eventType()).isEqualTo("CampaignCreated");
        assertThat(event.campaignId()).isEqualTo(campaignId);
        assertThat(event.name()).isEqualTo("Flood response in Granollers");
        assertThat(event.description()).isEqualTo("Volunteer coordination after heavy rain flooding");
        assertThat(event.location()).isEqualTo("Granollers");
        assertThat(event.priority()).isEqualTo(CampaignPriority.HIGH);
        assertThat(event.status()).isEqualTo(CampaignStatus.ACTIVE);
        assertThat(event.createdBy()).isEqualTo(createdBy);
        assertThat(event.createdAt()).isEqualTo(createdAt);
    }
}
