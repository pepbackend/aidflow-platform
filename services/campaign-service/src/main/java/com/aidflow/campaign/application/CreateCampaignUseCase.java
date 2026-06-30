package com.aidflow.campaign.application;

import com.aidflow.campaign.domain.model.Campaign;
import com.aidflow.campaign.domain.model.CampaignStatus;
import com.aidflow.campaign.domain.port.CampaignRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CreateCampaignUseCase {
    private final CampaignRepository campaignRepository;
    private final Clock clock;

    @Autowired
    public CreateCampaignUseCase(CampaignRepository campaignRepository) {
        this(campaignRepository, Clock.systemUTC());
    }

    public CreateCampaignUseCase(CampaignRepository campaignRepository, Clock clock) {
        this.campaignRepository = campaignRepository;
        this.clock = clock;
    }

    public Campaign execute(CreateCampaignCommand command) {
        if (!command.user().hasAnyRole("COORDINATOR", "ADMIN")) {
            throw new ForbiddenCampaignOperationException();
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        Campaign campaign = new Campaign(
                UUID.randomUUID(),
                command.name(),
                command.description(),
                command.location(),
                command.priority(),
                CampaignStatus.ACTIVE,
                command.user().id(),
                now,
                now
        );

        return campaignRepository.save(campaign);
    }
}
