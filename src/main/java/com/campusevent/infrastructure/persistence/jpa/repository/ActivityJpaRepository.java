package com.campusevent.infrastructure.persistence.jpa.repository;

import com.campusevent.infrastructure.persistence.jpa.entity.ActivityJpaEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityJpaRepository extends JpaRepository<ActivityJpaEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from ActivityJpaEntity a where a.activityId = :id")
    Optional<ActivityJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            select a from ActivityJpaEntity a
            where a.deletedAt is null
              and lower(a.title) like lower(:pattern)
            order by a.startAt asc
            """)
    List<ActivityJpaEntity> findVisible(@Param("pattern") String pattern);
}
