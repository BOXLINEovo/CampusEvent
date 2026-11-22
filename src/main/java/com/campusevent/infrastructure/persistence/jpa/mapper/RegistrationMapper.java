package com.campusevent.infrastructure.persistence.jpa.mapper;

import com.campusevent.domain.model.Registration;
import com.campusevent.infrastructure.persistence.jpa.entity.RegistrationJpaEntity;

public final class RegistrationMapper {

    private RegistrationMapper() {
    }

    public static Registration toDomain(RegistrationJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Registration(
                entity.getRegistrationId(),
                entity.getActivityId(),
                entity.getStudentId(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static RegistrationJpaEntity toEntity(Registration registration) {
        return new RegistrationJpaEntity(
                registration.getRegistrationId(),
                registration.getActivityId(),
                registration.getStudentId(),
                registration.getStatus(),
                registration.getCreatedAt(),
                registration.getUpdatedAt());
    }
}
