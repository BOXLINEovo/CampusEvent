package com.campusevent.infrastructure.persistence.jpa.mapper;

import com.campusevent.domain.model.ActivityGradeRuleV2;
import com.campusevent.infrastructure.persistence.jpa.entity.ActivityGradeRuleJpaEntityV2;

public final class ActivityGradeRuleMapperV2 {

    private ActivityGradeRuleMapperV2() {
    }

    public static ActivityGradeRuleV2 toDomain(ActivityGradeRuleJpaEntityV2 entity) {
        if (entity == null) {
            return null;
        }
        return new ActivityGradeRuleV2(
                entity.getActivityId(),
                entity.getMinGrade(),
                entity.getMaxGrade(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static ActivityGradeRuleJpaEntityV2 toEntity(ActivityGradeRuleV2 rule) {
        return new ActivityGradeRuleJpaEntityV2(
                rule.activityId(),
                (short) rule.minGrade(),
                (short) rule.maxGrade(),
                rule.createdAt(),
                rule.updatedAt());
    }
}
