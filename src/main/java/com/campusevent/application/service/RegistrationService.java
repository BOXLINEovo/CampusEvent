package com.campusevent.application.service;

import com.campusevent.application.dto.RegistrationResult;
import com.campusevent.application.exception.ActivityNotFoundException;
import com.campusevent.application.exception.ActivityNotOpenException;
import com.campusevent.application.exception.CapacityExceededException;
import com.campusevent.application.exception.ConcurrentStateChangeException;
import com.campusevent.application.exception.DuplicateActiveRegistrationException;
import com.campusevent.application.exception.NotRegistrationOwnerException;
import com.campusevent.application.exception.RegistrationNotFoundException;
import com.campusevent.application.policy.RegistrationPolicyFactory;
import com.campusevent.application.port.ActivityRepository;
import com.campusevent.application.port.RegistrationEventPublisher;
import com.campusevent.application.port.RegistrationRepository;
import com.campusevent.application.security.DemoAccounts;
import com.campusevent.domain.event.RegistrationEvent;
import com.campusevent.domain.exception.InvalidRegistrationStateException;
import com.campusevent.domain.model.Activity;
import com.campusevent.domain.model.Registration;
import com.campusevent.domain.model.RegistrationStatus;
import com.campusevent.domain.policy.RegistrationPolicy;
import com.campusevent.domain.rule.CapacityDecision;
import com.campusevent.domain.rule.CapacityRule;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration / cancellation / approval use-case orchestration.
 * No notification dependency: events are published through RegistrationEventPublisher.
 */
@Service
public class RegistrationService {

    private final ActivityRepository activityRepository;
    private final RegistrationRepository registrationRepository;
    private final RegistrationPolicyFactory policyFactory;
    private final CapacityRule capacityRule;
    private final RegistrationEventPublisher eventPublisher;

    public RegistrationService(
            ActivityRepository activityRepository,
            RegistrationRepository registrationRepository,
            RegistrationPolicyFactory policyFactory,
            CapacityRule capacityRule,
            RegistrationEventPublisher eventPublisher) {
        this.activityRepository = activityRepository;
        this.registrationRepository = registrationRepository;
        this.policyFactory = policyFactory;
        this.capacityRule = capacityRule;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public RegistrationResult register(UUID studentId, UUID activityId) {
        Activity activity = loadActivityForUpdate(activityId);
        Instant now = Instant.now();
        if (!activity.isOpenForRegistration(now)) {
            throw new ActivityNotOpenException("activity is not open for registration");
        }

        registrationRepository.findActiveBy(activityId, studentId).ifPresent(existing -> {
            throw new DuplicateActiveRegistrationException("active registration already exists");
        });

        RegistrationPolicy policy = policyFactory.create(activity.getPolicyType());
        RegistrationStatus initialStatus = policy.initialStatus(activity);
        long approvedCount = registrationRepository.countApproved(activityId);
        CapacityDecision decision = capacityRule.checkSubmission(activity, approvedCount, initialStatus);
        if (!decision.allowed()) {
            throw new CapacityExceededException(decision.message());
        }

        Registration registration = Registration.create(activityId, studentId, initialStatus, now);
        registrationRepository.insert(registration);

        RegistrationEvent event = initialStatus == RegistrationStatus.APPROVED
                ? RegistrationEvent.registered(registration, activity, studentId, now)
                : RegistrationEvent.submitted(registration, activity, studentId, now);
        eventPublisher.publish(event);

        return new RegistrationResult(registration, decision.warning() ? decision.message() : null);
    }

    @Transactional
    public Registration approve(UUID registrationId) {
        Registration registration = loadRegistration(registrationId);
        if (registration.getStatus() != RegistrationStatus.PENDING) {
            throw new InvalidRegistrationStateException("only PENDING registration can be approved");
        }

        Activity activity = loadActivityForUpdate(registration.getActivityId());
        Instant now = Instant.now();
        if (activity.isDeleted() || activity.hasStarted(now)) {
            throw new ActivityNotOpenException("activity is not open for approval");
        }

        long approvedCount = registrationRepository.countApproved(activity.getActivityId());
        CapacityDecision decision = capacityRule.checkApproval(activity, approvedCount);
        if (!decision.allowed()) {
            throw new CapacityExceededException(decision.message());
        }

        registration.approve(now);
        boolean updated = registrationRepository.updateStatusIfCurrent(
                registrationId,
                RegistrationStatus.PENDING,
                RegistrationStatus.APPROVED,
                now);
        if (!updated) {
            throw new ConcurrentStateChangeException("registration status changed concurrently");
        }

        eventPublisher.publish(RegistrationEvent.approved(registration, activity, DemoAccounts.ADMIN, now));
        return registration;
    }

    @Transactional
    public Registration reject(UUID registrationId, String reason) {
        Registration registration = loadRegistration(registrationId);
        if (registration.getStatus() != RegistrationStatus.PENDING) {
            throw new InvalidRegistrationStateException("only PENDING registration can be rejected");
        }

        String activityTitle = activityRepository.findById(registration.getActivityId())
                .map(Activity::getTitle)
                .orElse("活动");
        Instant now = Instant.now();
        registration.reject(now, reason);
        boolean updated = registrationRepository.updateStatusIfCurrent(
                registrationId,
                RegistrationStatus.PENDING,
                RegistrationStatus.REJECTED,
                now);
        if (!updated) {
            throw new ConcurrentStateChangeException("registration status changed concurrently");
        }

        eventPublisher.publish(RegistrationEvent.rejected(
                registration,
                activityTitle,
                DemoAccounts.ADMIN,
                reason,
                now));
        return registration;
    }

    @Transactional
    public void cancel(UUID studentId, UUID registrationId) {
        Registration registration = loadRegistration(registrationId);
        if (!registration.getStudentId().equals(studentId)) {
            throw new NotRegistrationOwnerException("registration does not belong to the student");
        }
        RegistrationStatus fromStatus = registration.getStatus();
        if (fromStatus != RegistrationStatus.PENDING && fromStatus != RegistrationStatus.APPROVED) {
            throw new InvalidRegistrationStateException("only PENDING or APPROVED registration can be cancelled");
        }

        String activityTitle = activityRepository.findById(registration.getActivityId())
                .map(Activity::getTitle)
                .orElse("活动");
        Instant now = Instant.now();
        registration.cancel(now);
        boolean updated = registrationRepository.updateStatusIfCurrent(
                registrationId,
                fromStatus,
                RegistrationStatus.CANCELLED,
                now);
        if (!updated) {
            throw new ConcurrentStateChangeException("registration status changed concurrently");
        }

        eventPublisher.publish(RegistrationEvent.cancelled(
                registration,
                activityTitle,
                studentId,
                fromStatus,
                now));
    }

    @Transactional(readOnly = true)
    public List<Registration> listByStudent(UUID studentId) {
        return registrationRepository.findByStudent(studentId);
    }

    @Transactional(readOnly = true)
    public List<Registration> listPendingByActivity(UUID activityId) {
        return registrationRepository.findPendingByActivity(activityId);
    }

    private Activity loadActivityForUpdate(UUID activityId) {
        return activityRepository.findByIdForUpdate(activityId)
                .orElseThrow(() -> new ActivityNotFoundException("activity not found: " + activityId));
    }

    private Registration loadRegistration(UUID registrationId) {
        return registrationRepository.findById(registrationId)
                .orElseThrow(() -> new RegistrationNotFoundException("registration not found: " + registrationId));
    }
}
