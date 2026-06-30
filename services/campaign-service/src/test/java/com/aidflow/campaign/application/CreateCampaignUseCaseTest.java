package com.aidflow.campaign.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aidflow.campaign.domain.model.Campaign;
import com.aidflow.campaign.domain.model.CampaignPriority;
import com.aidflow.campaign.domain.model.CampaignStatus;
import com.aidflow.campaign.domain.port.CampaignRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateCampaignUseCaseTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-30T10:15:30Z"), ZoneOffset.UTC);

    @Test
    void createsCampaignAsActiveAndRecordsCampaignCreatedEvent() {
        InMemoryCampaignRepository repository = new InMemoryCampaignRepository();
        RecordingCampaignEventRecorder eventRecorder = new RecordingCampaignEventRecorder();
        CreateCampaignUseCase useCase = new CreateCampaignUseCase(repository, eventRecorder, FIXED_CLOCK);
        UUID userId = UUID.randomUUID();

        Campaign campaign = useCase.execute(new CreateCampaignCommand(
                "Flood response in Granollers",
                "Volunteer coordination after heavy rain flooding",
                "Granollers",
                CampaignPriority.HIGH,
                new AuthenticatedUser(userId, "coordinator@example.com", Set.of("COORDINATOR"))
        ));

        assertThat(campaign.id()).isNotNull();
        assertThat(campaign.status()).isEqualTo(CampaignStatus.ACTIVE);
        assertThat(campaign.createdBy()).isEqualTo(userId);
        assertThat(campaign.createdAt()).isEqualTo(Instant.parse("2026-06-30T10:15:30Z").atOffset(ZoneOffset.UTC));
        assertThat(campaign.updatedAt()).isEqualTo(campaign.createdAt());
        assertThat(repository.saved).containsExactly(campaign);
        assertThat(eventRecorder.campaignsCreated).containsExactly(campaign);
    }

    @Test
    void rejectsUsersWithoutCoordinatorOrAdminRole() {
        InMemoryCampaignRepository repository = new InMemoryCampaignRepository();
        RecordingCampaignEventRecorder eventRecorder = new RecordingCampaignEventRecorder();
        CreateCampaignUseCase useCase = new CreateCampaignUseCase(repository, eventRecorder, FIXED_CLOCK);

        CreateCampaignCommand command = new CreateCampaignCommand(
                "Flood response in Granollers",
                "Volunteer coordination after heavy rain flooding",
                "Granollers",
                CampaignPriority.HIGH,
                new AuthenticatedUser(UUID.randomUUID(), "volunteer@example.com", Set.of("VOLUNTEER"))
        );

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(ForbiddenCampaignOperationException.class);
        assertThat(repository.saved).isEmpty();
        assertThat(eventRecorder.campaignsCreated).isEmpty();
    }

    private static class RecordingCampaignEventRecorder implements CampaignEventRecorder {
        private final List<Campaign> campaignsCreated = new ArrayList<>();

        @Override
        public void recordCampaignCreated(Campaign campaign) {
            campaignsCreated.add(campaign);
        }
    }

    private static class InMemoryCampaignRepository implements CampaignRepository {
        private final List<Campaign> saved = new ArrayList<>();

        @Override
        public Campaign save(Campaign campaign) {
            saved.add(campaign);
            return campaign;
        }

        @Override
        public List<Campaign> findAll() {
            return saved;
        }

        @Override
        public Optional<Campaign> findById(UUID id) {
            return saved.stream()
                    .filter(campaign -> campaign.id().equals(id))
                    .findFirst();
        }
    }
}
