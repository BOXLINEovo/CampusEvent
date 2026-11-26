package com.campusevent.infrastructure.persistence.jpa.entity;

import com.campusevent.domain.model.EmailNotificationStatusV2;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_notification_v2")
public class EmailNotificationJpaEntityV2 {

    @Id
    @Column(name = "email_id", nullable = false)
    private UUID emailId;

    @Column(name = "source_event_id", nullable = false)
    private UUID sourceEventId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "recipient", nullable = false, length = 320)
    private String recipient;

    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EmailNotificationStatusV2 status;

    @Column(name = "error", columnDefinition = "text")
    private String error;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EmailNotificationJpaEntityV2() {
    }

    public EmailNotificationJpaEntityV2(
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
        this.emailId = emailId;
        this.sourceEventId = sourceEventId;
        this.studentId = studentId;
        this.recipient = recipient;
        this.subject = subject;
        this.content = content;
        this.status = status;
        this.error = error;
        this.sentAt = sentAt;
        this.createdAt = createdAt;
    }

    public UUID getEmailId() {
        return emailId;
    }

    public UUID getSourceEventId() {
        return sourceEventId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }

    public EmailNotificationStatusV2 getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
