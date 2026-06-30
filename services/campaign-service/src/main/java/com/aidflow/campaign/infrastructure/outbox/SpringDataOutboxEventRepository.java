package com.aidflow.campaign.infrastructure.outbox;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataOutboxEventRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {
    List<OutboxEventJpaEntity> findByStatusOrderByCreatedAtAsc(OutboxEventStatus status, Pageable pageable);
}
