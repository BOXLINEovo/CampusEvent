package com.campusevent.infrastructure.persistence.jpa.entity;

import com.campusevent.domain.model.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "message")
public class MessageJpaEntity {

    @Id
    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "activity_id", nullable = false)
    private UUID activityId;

    @Column(name = "source_event_id", nullable = false)
    private UUID sourceEventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    protected MessageJpaEntity() {
    }

    public MessageJpaEntity(
            UUID messageId,
            UUID studentId,
            UUID activityId,
            UUID sourceEventId,
            NotificationType type,
            String title,
            String content,
            boolean read,
            Instant createdAt,
            Instant readAt) {
        this.messageId = messageId;
        this.studentId = studentId;
        this.activityId = activityId;
        this.sourceEventId = sourceEventId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.read = read;
        this.createdAt = createdAt;
        this.readAt = readAt;
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

    public boolean isRead() {
        return read;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }
}
