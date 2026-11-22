package com.campusevent.infrastructure.persistence.jpa.adapter;

import com.campusevent.application.port.OutboxRepository;
import com.campusevent.domain.event.OutboxEvent;
import com.campusevent.domain.event.OutboxStatus;
import com.campusevent.infrastructure.persistence.jpa.entity.OutboxEventJpaEntity;
import com.campusevent.infrastructure.persistence.jpa.mapper.OutboxEventMapper;
import com.campusevent.infrastructure.persistence.jpa.repository.OutboxEventJpaRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaOutboxRepository implements OutboxRepository {

    private static final int MAX_RETRIES = 5;

    private final OutboxEventJpaRepository jpaRepository;

    public JpaOutboxRepository(OutboxEventJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(OutboxEvent event) {
        jpaRepository.save(OutboxEventMapper.toEntity(event));
    }

    @Override
    @Transactional
    public List<OutboxEvent> findPending(int limit) {
        Instant now = Instant.now();
        // A worker crash can leave an event in PROCESSING. Reclaim after a 5-minute lease.
        jpaRepository.reclaimStaleProcessing(
                OutboxStatus.PROCESSING,
                OutboxStatus.PENDING,
                now.minus(Duration.ofMinutes(5)),
                now);
        int safeLimit = Math.max(1, limit);
        PageRequest page = PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.ASC, "createdAt"));
        return jpaRepository.findDue(OutboxStatus.PENDING, now, page).stream()
                .map(OutboxEventMapper::toDomain)
                .toList();
    }

    @Override
    public boolean markProcessing(UUID eventId) {
        return jpaRepository.markProcessing(
                eventId,
                OutboxStatus.PENDING,
                OutboxStatus.PROCESSING,
                Instant.now()) == 1;
    }

    @Override
    public void markSent(UUID eventId) {
        jpaRepository.markSent(eventId, OutboxStatus.SENT, Instant.now());
    }

    @Override
    public void markFailed(UUID eventId, String error, Instant nextAttemptAt) {
        OutboxEventJpaEntity entity = jpaRepository.findById(eventId).orElse(null);
        if (entity == null) {
            return;
        }
        int nextRetry = entity.getRetryCount() + 1;
        entity.setRetryCount(nextRetry);
        entity.setLastError(error);
        entity.setNextAttemptAt(nextAttemptAt);
        entity.setUpdatedAt(Instant.now());
        entity.setStatus(nextRetry >= MAX_RETRIES ? OutboxStatus.FAILED : OutboxStatus.PENDING);
        jpaRepository.save(entity);
    }
}
