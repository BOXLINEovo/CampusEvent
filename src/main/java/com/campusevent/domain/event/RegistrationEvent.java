package com.campusevent.domain.event;

import com.campusevent.domain.model.Activity;
import com.campusevent.domain.model.ActorRole;
import com.campusevent.domain.model.PolicyType;
import com.campusevent.domain.model.Registration;
import com.campusevent.domain.model.RegistrationStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event raised by registration/approval state changes.
 * fromStatus, reason and studentName may be null.
 */
public record RegistrationEvent(
        UUID eventId,
        int schemaVersion,
        RegistrationEventType type,
        UUID registrationId,
        UUID activityId,
        UUID studentId,
        PolicyType policyType,
        RegistrationStatus fromStatus,
        RegistrationStatus toStatus,
        UUID actorId,
        ActorRole actorRole,
        String reason,
        String activityTitle,
        String studentName,
        Instant occurredAt) {

    public static RegistrationEvent registered(
            Registration registration,
            Activity activity,
            UUID actorId,
            Instant now) {
        return new RegistrationEvent(
                UUID.randomUUID(),
                1,
                RegistrationEventType.REGISTERED,
                registration.getRegistrationId(),
                registration.getActivityId(),
                registration.getStudentId(),
                activity.getPolicyType(),
                null,
                RegistrationStatus.APPROVED,
                actorId,
                ActorRole.STUDENT,
                null,
                activity.getTitle(),
                null,
                now);
    }

    public static RegistrationEvent submitted(
            Registration registration,
            Activity activity,
            UUID actorId,
            Instant now) {
        return new RegistrationEvent(
                UUID.randomUUID(),
                1,
                RegistrationEventType.SUBMITTED,
                registration.getRegistrationId(),
                registration.getActivityId(),
                registration.getStudentId(),
                activity.getPolicyType(),
                null,
                RegistrationStatus.PENDING,
                actorId,
                ActorRole.STUDENT,
                null,
                activity.getTitle(),
                null,
                now);
    }

    public static RegistrationEvent approved(
            Registration registration,
            Activity activity,
            UUID actorId,
            Instant now) {
        return new RegistrationEvent(
                UUID.randomUUID(),
                1,
                RegistrationEventType.APPROVED,
                registration.getRegistrationId(),
                registration.getActivityId(),
                registration.getStudentId(),
                activity.getPolicyType(),
                RegistrationStatus.PENDING,
                RegistrationStatus.APPROVED,
                actorId,
                ActorRole.ADMIN,
                null,
                activity.getTitle(),
                null,
                now);
    }

    public static RegistrationEvent rejected(
            Registration registration,
            String activityTitle,
            UUID actorId,
            String reason,
            Instant now) {
        return new RegistrationEvent(
                UUID.randomUUID(),
                1,
                RegistrationEventType.REJECTED,
                registration.getRegistrationId(),
                registration.getActivityId(),
                registration.getStudentId(),
                null,
                RegistrationStatus.PENDING,
                RegistrationStatus.REJECTED,
                actorId,
                ActorRole.ADMIN,
                reason,
                activityTitle,
                null,
                now);
    }

    public static RegistrationEvent cancelled(
            Registration registration,
            String activityTitle,
            UUID actorId,
            RegistrationStatus fromStatus,
            Instant now) {
        return new RegistrationEvent(
                UUID.randomUUID(),
                1,
                RegistrationEventType.CANCELLED,
                registration.getRegistrationId(),
                registration.getActivityId(),
                registration.getStudentId(),
                null,
                fromStatus,
                RegistrationStatus.CANCELLED,
                actorId,
                ActorRole.STUDENT,
                null,
                activityTitle,
                null,
                now);
    }
}
