package com.campusevent.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * In-site message entity.
 */
public class Message {

    private final UUID messageId;
    private final UUID studentId;
    private final UUID activityId;
    private final UUID sourceEventId;
    private final NotificationType type;
    private final String title;
    private final String content;
    private final Instant createdAt;
    private boolean read;
    private Instant readAt;

    public Message(
            UUID messageId,
            UUID studentId,
            UUID activityId,
            UUID sourceEventId,
            NotificationType type,
            String title,
            String content,
            Instant createdAt,
            boolean read,
            Instant readAt) {
        this.messageId = messageId;
        this.studentId = studentId;
        this.activityId = activityId;
        this.sourceEventId = sourceEventId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.read = read;
        this.readAt = readAt;
    }

    public static Message create(
            UUID studentId,
            UUID activityId,
            UUID sourceEventId,
            NotificationType type,
            String title,
            String content,
            Instant now) {
        return new Message(
                UUID.randomUUID(),
                studentId,
                activityId,
                sourceEventId,
                type,
                title,
                content,
                now,
                false,
                null);
    }

    public void markRead(Instant now) {
        if (read) {
            return;
        }
        read = true;
        readAt = now;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public UUID getActivityId() {
        return activityId;
    }

    public UUID getSourceEventId() {
        return sourceEventId;
    }

    public NotificationType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isRead() {
        return read;
    }

    public Instant getReadAt() {
        return readAt;
    }
}
