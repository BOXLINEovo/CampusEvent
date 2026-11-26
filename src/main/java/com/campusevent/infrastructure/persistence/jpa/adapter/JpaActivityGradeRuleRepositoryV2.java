package com.campusevent.infrastructure.persistence.jpa.adapter;

import com.campusevent.application.port.ActivityGradeRuleRepositoryV2;
import com.campusevent.domain.model.ActivityGradeRuleV2;
import com.campusevent.infrastructure.persistence.jpa.mapper.ActivityGradeRuleMapperV2;
import com.campusevent.infrastructure.persistence.jpa.repository.ActivityGradeRuleJpaRepositoryV2;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaActivityGradeRuleRepositoryV2 implements ActivityGradeRuleRepositoryV2 {

    private final ActivityGradeRuleJpaRepositoryV2 jpaRepository;

    public JpaActivityGradeRuleRepositoryV2(ActivityGradeRuleJpaRepositoryV2 jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<ActivityGradeRuleV2> findByActivityId(UUID activityId) {
        return jpaRepository.findByActivityId(activityId)
                .map(ActivityGradeRuleMapperV2::toDomain);
    }

    @Override
    public List<ActivityGradeRuleV2> findAll() {
        return jpaRepository.findAllByOrderByActivityIdAsc().stream()
                .map(ActivityGradeRuleMapperV2::toDomain)
                .toList();
    }

    @Override
    public void save(ActivityGradeRuleV2 rule) {
        jpaRepository.save(ActivityGradeRuleMapperV2.toEntity(rule));
    }
}
