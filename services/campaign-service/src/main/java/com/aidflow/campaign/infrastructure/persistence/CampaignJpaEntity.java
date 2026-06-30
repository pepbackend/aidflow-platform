package com.aidflow.campaign.infrastructure.persistence;

import com.aidflow.campaign.domain.model.Campaign;
import com.aidflow.campaign.domain.model.CampaignPriority;
import com.aidflow.campaign.domain.model.CampaignStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "campaigns")
public class CampaignJpaEntity {
    @Id
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 150)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CampaignPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CampaignStatus status;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected CampaignJpaEntity() {
    }

    private CampaignJpaEntity(
            UUID id,
            String name,
            String description,
            String location,
            CampaignPriority priority,
            CampaignStatus status,
            UUID createdBy,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.location = location;
        this.priority = priority;
        this.status = status;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CampaignJpaEntity fromDomain(Campaign campaign) {
        return new CampaignJpaEntity(
                campaign.id(),
                campaign.name(),
                campaign.description(),
                campaign.location(),
                campaign.priority(),
                campaign.status(),
                campaign.createdBy(),
                campaign.createdAt(),
                campaign.updatedAt()
        );
    }

    public Campaign toDomain() {
        return new Campaign(
                id,
                name,
                description,
                location,
                priority,
                status,
                createdBy,
                createdAt,
                updatedAt
        );
    }
}
