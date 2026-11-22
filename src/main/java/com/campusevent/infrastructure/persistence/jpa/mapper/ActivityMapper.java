package com.campusevent.infrastructure.persistence.jpa.mapper;

import com.campusevent.domain.model.Activity;
import com.campusevent.infrastructure.persistence.jpa.entity.ActivityJpaEntity;

public final class ActivityMapper {

    private ActivityMapper() {
    }

    public static Activity toDomain(ActivityJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Activity(
                entity.getActivityId(),
                entity.getTitle(),
                entity.getCategory(),
                entity.getStartAt(),
                entity.getLocation(),
                entity.getCapacity(),
                entity.getPolicyType(),
                entity.getDeletedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static ActivityJpaEntity toEntity(Activity activity) {
        return new ActivityJpaEntity(
                activity.getActivityId(),
                activity.getTitle(),
                activity.getCategory(),
                activity.getStartAt(),
                activity.getLocation(),
                activity.getCapacity(),
                activity.getPolicyType(),
                activity.getDeletedAt(),
                activity.getCreatedAt(),
                activity.getUpdatedAt());
    }
}
