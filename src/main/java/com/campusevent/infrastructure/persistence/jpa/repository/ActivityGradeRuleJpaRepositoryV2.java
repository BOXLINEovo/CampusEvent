package com.campusevent.infrastructure.persistence.jpa.repository;

import com.campusevent.infrastructure.persistence.jpa.entity.ActivityGradeRuleJpaEntityV2;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityGradeRuleJpaRepositoryV2
        extends JpaRepository<ActivityGradeRuleJpaEntityV2, UUID> {

    Optional<ActivityGradeRuleJpaEntityV2> findByActivityId(UUID activityId);

    List<ActivityGradeRuleJpaEntityV2> findAllByOrderByActivityIdAsc();
}
