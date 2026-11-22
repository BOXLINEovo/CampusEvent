package com.campusevent.infrastructure.persistence.jpa.entity;

import com.campusevent.domain.model.RegistrationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "registration")
public class RegistrationJpaEntity {

    @Id
    @Column(name = "registration_id", nullable = false)
    private UUID registrationId;

    @Column(name = "activity_id", nullable = false)
    private UUID activityId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RegistrationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RegistrationJpaEntity() {
    }

    public RegistrationJpaEntity(
            UUID registrationId,
            UUID activityId,
            UUID studentId,
            RegistrationStatus status,
            Instant createdAt,
            Instant updatedAt) {
        this.registrationId = registrationId;
        this.activityId = activityId;
        this.studentId = studentId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getRegistrationId() {
        return registrationId;
    }

    public UUID getActivityId() {
        return activityId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public RegistrationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
