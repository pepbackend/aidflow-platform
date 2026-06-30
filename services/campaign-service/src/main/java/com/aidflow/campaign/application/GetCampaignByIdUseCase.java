package com.aidflow.campaign.application;

import com.aidflow.campaign.domain.model.Campaign;
import com.aidflow.campaign.domain.port.CampaignRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GetCampaignByIdUseCase {
    private final CampaignRepository campaignRepository;

    public GetCampaignByIdUseCase(CampaignRepository campaignRepository) {
        this.campaignRepository = campaignRepository;
    }

    public Campaign execute(UUID id) {
        return campaignRepository.findById(id)
                .orElseThrow(() -> new CampaignNotFoundException(id));
    }
}
