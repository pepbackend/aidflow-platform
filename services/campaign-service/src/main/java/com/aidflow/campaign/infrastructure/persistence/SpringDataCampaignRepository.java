package com.aidflow.campaign.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCampaignRepository extends JpaRepository<CampaignJpaEntity, UUID> {
}
