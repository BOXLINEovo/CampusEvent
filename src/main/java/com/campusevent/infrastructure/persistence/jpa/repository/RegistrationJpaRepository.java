package com.campusevent.infrastructure.persistence.jpa.repository;

import com.campusevent.domain.model.RegistrationStatus;
import com.campusevent.infrastructure.persistence.jpa.entity.RegistrationJpaEntity;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegistrationJpaRepository extends JpaRepository<RegistrationJpaEntity, UUID> {

    @Query("""
            select r from RegistrationJpaEntity r
            where r.activityId = :activityId
              and r.studentId = :studentId
              and r.status in :statuses
            """)
    Optional<RegistrationJpaEntity> findActive(
            @Param("activityId") UUID activityId,
            @Param("studentId") UUID studentId,
            @Param("statuses") Collection<RegistrationStatus> statuses);

    @Query("""
            select count(r) from RegistrationJpaEntity r
            where r.activityId = :activityId
              and r.status = :status
            """)
    long countByActivityIdAndStatus(
            @Param("activityId") UUID activityId,
            @Param("status") RegistrationStatus status);

    List<RegistrationJpaEntity> findByActivityIdAndStatusOrderByCreatedAtAsc(
            UUID activityId,
            RegistrationStatus status);

    List<RegistrationJpaEntity> findByStudentIdOrderByCreatedAtDesc(UUID studentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RegistrationJpaEntity r
               set r.status = :target,
                   r.updatedAt = :now
             where r.registrationId = :id
               and r.status = :expected
            """)
    int updateStatusIfCurrent(
            @Param("id") UUID registrationId,
            @Param("expected") RegistrationStatus expected,
            @Param("target") RegistrationStatus target,
            @Param("now") Instant now);
}
