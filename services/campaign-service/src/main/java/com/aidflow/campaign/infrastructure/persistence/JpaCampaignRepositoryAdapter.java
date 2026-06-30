package com.aidflow.campaign.infrastructure.persistence;

import com.aidflow.campaign.domain.model.Campaign;
import com.aidflow.campaign.domain.port.CampaignRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCampaignRepositoryAdapter implements CampaignRepository {
    private final SpringDataCampaignRepository springDataCampaignRepository;

    public JpaCampaignRepositoryAdapter(SpringDataCampaignRepository springDataCampaignRepository) {
        this.springDataCampaignRepository = springDataCampaignRepository;
    }

    @Override
    public Campaign save(Campaign campaign) {
        return springDataCampaignRepository.save(CampaignJpaEntity.fromDomain(campaign)).toDomain();
    }

    @Override
    public List<Campaign> findAll() {
        return springDataCampaignRepository.findAll().stream()
                .map(CampaignJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Campaign> findById(UUID id) {
        return springDataCampaignRepository.findById(id)
                .map(CampaignJpaEntity::toDomain);
    }
}
