package com.campusevent.domain.model;

import com.campusevent.domain.exception.InvalidRegistrationStateException;
import java.time.Instant;
import java.util.UUID;

/**
 * Registration aggregate root with a guarded status machine.
 */
public class Registration {

    private final UUID registrationId;
    private final UUID activityId;
    private final UUID studentId;
    private RegistrationStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public Registration(
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

    public static Registration create(
            UUID activityId,
            UUID studentId,
            RegistrationStatus status,
            Instant now) {
        return new Registration(UUID.randomUUID(), activityId, studentId, status, now, now);
    }

    public void approve(Instant now) {
        if (status != RegistrationStatus.PENDING) {
            throw new InvalidRegistrationStateException("Only PENDING registration can be approved");
        }
        status = RegistrationStatus.APPROVED;
        updatedAt = now;
    }

    public void reject(Instant now, String reason) {
        if (status != RegistrationStatus.PENDING) {
            throw new InvalidRegistrationStateException("Only PENDING registration can be rejected");
        }
        // Reason is carried by the domain event/message; it is not stored on the registration row.
        status = RegistrationStatus.REJECTED;
        updatedAt = now;
    }

    public void cancel(Instant now) {
        if (status != RegistrationStatus.PENDING && status != RegistrationStatus.APPROVED) {
            throw new InvalidRegistrationStateException("Only PENDING or APPROVED registration can be cancelled");
        }
        status = RegistrationStatus.CANCELLED;
        updatedAt = now;
    }

    public boolean isActive() {
        return status == RegistrationStatus.PENDING || status == RegistrationStatus.APPROVED;
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
