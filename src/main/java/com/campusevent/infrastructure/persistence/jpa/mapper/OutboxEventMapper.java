package com.campusevent.infrastructure.persistence.jpa.mapper;

import com.campusevent.domain.event.OutboxEvent;
import com.campusevent.infrastructure.persistence.jpa.entity.OutboxEventJpaEntity;

public final class OutboxEventMapper {

    private OutboxEventMapper() {
    }

    public static OutboxEvent toDomain(OutboxEventJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new OutboxEvent(
                entity.getEventId(),
                entity.getType(),
                entity.getPayload(),
                entity.getStatus(),
                entity.getRetryCount(),
                entity.getNextAttemptAt(),
                entity.getLastError(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static OutboxEventJpaEntity toEntity(OutboxEvent event) {
        return new OutboxEventJpaEntity(
                event.getEventId(),
                event.getType(),
                event.getPayload(),
                event.getStatus(),
                event.getRetryCount(),
                event.getNextAttemptAt(),
                event.getLastError(),
                event.getCreatedAt(),
                event.getUpdatedAt());
    }
}
