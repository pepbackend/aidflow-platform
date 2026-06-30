package com.aidflow.campaign.domain.port;

import com.aidflow.campaign.domain.model.Campaign;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampaignRepository {
    Campaign save(Campaign campaign);

    List<Campaign> findAll();

    Optional<Campaign> findById(UUID id);
}
