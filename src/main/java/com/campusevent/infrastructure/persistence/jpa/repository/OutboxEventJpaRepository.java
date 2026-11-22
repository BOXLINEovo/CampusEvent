package com.campusevent.infrastructure.persistence.jpa.repository;

import com.campusevent.domain.event.OutboxStatus;
import com.campusevent.infrastructure.persistence.jpa.entity.OutboxEventJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

    @Query("""
            select e from OutboxEventJpaEntity e
             where e.status = :pending
               and (e.nextAttemptAt is null or e.nextAttemptAt <= :now)
             order by e.createdAt asc
            """)
    List<OutboxEventJpaEntity> findDue(
            @Param("pending") OutboxStatus pending,
            @Param("now") Instant now,
            Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update OutboxEventJpaEntity e
               set e.status = :pending,
                   e.updatedAt = :now
             where e.status = :processing
               and e.updatedAt < :staleBefore
            """)
    int reclaimStaleProcessing(
            @Param("processing") OutboxStatus processing,
            @Param("pending") OutboxStatus pending,
            @Param("staleBefore") Instant staleBefore,
            @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update OutboxEventJpaEntity e
               set e.status = :processing,
                   e.updatedAt = :now
             where e.eventId = :eventId
               and e.status = :pending
            """)
    int markProcessing(
            @Param("eventId") UUID eventId,
            @Param("pending") OutboxStatus pending,
            @Param("processing") OutboxStatus processing,
            @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update OutboxEventJpaEntity e
               set e.status = :sent,
                   e.updatedAt = :now
             where e.eventId = :eventId
            """)
    int markSent(
            @Param("eventId") UUID eventId,
            @Param("sent") OutboxStatus sent,
            @Param("now") Instant now);
}
