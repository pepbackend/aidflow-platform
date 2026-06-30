package com.aidflow.campaign.application;

import com.aidflow.campaign.domain.model.Campaign;
import com.aidflow.campaign.domain.port.CampaignRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GetCampaignsUseCase {
    private final CampaignRepository campaignRepository;

    public GetCampaignsUseCase(CampaignRepository campaignRepository) {
        this.campaignRepository = campaignRepository;
    }

    public List<Campaign> execute() {
        return campaignRepository.findAll();
    }
}
