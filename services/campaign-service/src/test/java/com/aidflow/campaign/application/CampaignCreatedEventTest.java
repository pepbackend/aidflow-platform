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
        assertThat(event.aggregateId()).isEqualTo(campaignId);
        assertThat(event.aggregateType()).isEqualTo("Campaign");
        assertThat(event.occurredAt()).isEqualTo(createdAt);
        assertThat(event.version()).isEqualTo(1);
        assertThat(event.payload().campaignId()).isEqualTo(campaignId);
        assertThat(event.payload().name()).isEqualTo("Flood response in Granollers");
        assertThat(event.payload().description()).isEqualTo("Volunteer coordination after heavy rain flooding");
        assertThat(event.payload().location()).isEqualTo("Granollers");
        assertThat(event.payload().priority()).isEqualTo(CampaignPriority.HIGH);
        assertThat(event.payload().status()).isEqualTo(CampaignStatus.ACTIVE);
        assertThat(event.payload().createdBy()).isEqualTo(createdBy);
    }
}
