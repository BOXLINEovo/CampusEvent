package com.campusevent.infrastructure.persistence.jpa.mapper;

import com.campusevent.domain.model.Message;
import com.campusevent.infrastructure.persistence.jpa.entity.MessageJpaEntity;

public final class MessageMapper {

    private MessageMapper() {
    }

    public static Message toDomain(MessageJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Message(
                entity.getMessageId(),
                entity.getStudentId(),
                entity.getActivityId(),
                entity.getSourceEventId(),
                entity.getType(),
                entity.getTitle(),
                entity.getContent(),
                entity.getCreatedAt(),
                entity.isRead(),
                entity.getReadAt());
    }

    public static MessageJpaEntity toEntity(Message message) {
        return new MessageJpaEntity(
                message.getMessageId(),
                message.getStudentId(),
                message.getActivityId(),
                message.getSourceEventId(),
                message.getType(),
                message.getTitle(),
                message.getContent(),
                message.isRead(),
                message.getCreatedAt(),
                message.getReadAt());
    }
}
