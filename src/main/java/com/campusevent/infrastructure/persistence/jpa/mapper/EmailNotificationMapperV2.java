package com.campusevent.infrastructure.persistence.jpa.mapper;

import com.campusevent.domain.model.EmailNotificationV2;
import com.campusevent.infrastructure.persistence.jpa.entity.EmailNotificationJpaEntityV2;

public final class EmailNotificationMapperV2 {

    private EmailNotificationMapperV2() {
    }

    public static EmailNotificationV2 toDomain(EmailNotificationJpaEntityV2 entity) {
        if (entity == null) {
            return null;
        }
        return new EmailNotificationV2(
                entity.getEmailId(),
                entity.getSourceEventId(),
                entity.getStudentId(),
                entity.getRecipient(),
                entity.getSubject(),
                entity.getContent(),
                entity.getStatus(),
                entity.getError(),
                entity.getSentAt(),
                entity.getCreatedAt());
    }

    public static EmailNotificationJpaEntityV2 toEntity(EmailNotificationV2 notification) {
        return new EmailNotificationJpaEntityV2(
                notification.emailId(),
                notification.sourceEventId(),
                notification.studentId(),
                notification.recipient(),
                notification.subject(),
                notification.content(),
                notification.status(),
                notification.error(),
                notification.sentAt(),
                notification.createdAt());
    }
}
