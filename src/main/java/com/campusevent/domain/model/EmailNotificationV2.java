package com.campusevent.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Persisted mock-email record. The unique key (sourceEventId, studentId) is enforced by the database.
 */
public record EmailNotificationV2(
        UUID emailId,
        UUID sourceEventId,
        UUID studentId,
        String recipient,
        String subject,
        String content,
        EmailNotificationStatusV2 status,
        String error,
        Instant sentAt,
        Instant createdAt) {

    public static EmailNotificationV2 sent(
            UUID sourceEventId,
            UUID studentId,
            String recipient,
            String subject,
            String content,
            Instant now) {
        return new EmailNotificationV2(
                UUID.randomUUID(),
                sourceEventId,
                studentId,
                recipient,
                subject,
                content,
                EmailNotificationStatusV2.SENT,
                null,
                now,
                now);
    }

    public static EmailNotificationV2 failed(
            UUID sourceEventId,
            UUID studentId,
            String recipient,
            String subject,
            String content,
            String error,
            Instant now) {
        return new EmailNotificationV2(
                UUID.randomUUID(),
                sourceEventId,
                studentId,
                recipient,
                subject,
                content,
                EmailNotificationStatusV2.FAILED,
                error,
                null,
                now);
    }
}
