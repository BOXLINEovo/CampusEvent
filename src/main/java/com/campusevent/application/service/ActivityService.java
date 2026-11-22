package com.campusevent.application.service;

import com.campusevent.application.exception.ActivityNotDeletableException;
import com.campusevent.application.exception.ActivityNotFoundException;
import com.campusevent.application.exception.InvalidActivityException;
import com.campusevent.application.port.ActivityRepository;
import com.campusevent.application.port.RegistrationRepository;
import com.campusevent.domain.model.Activity;
import com.campusevent.domain.model.PolicyType;
import com.campusevent.domain.model.Registration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Activity management / browsing use cases.
 */
@Service
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final RegistrationRepository registrationRepository;

    public ActivityService(ActivityRepository activityRepository, RegistrationRepository registrationRepository) {
        this.activityRepository = activityRepository;
        this.registrationRepository = registrationRepository;
    }

    @Transactional
    public Activity create(
            String title,
            String category,
            Instant startAt,
            String location,
            int capacity,
            PolicyType policyType) {
        validate(title, category, startAt, location, capacity, policyType, null);
        Instant now = Instant.now();
        Activity activity = new Activity(
                UUID.randomUUID(),
                title.trim(),
                category.trim(),
                startAt,
                location.trim(),
                capacity,
                policyType,
                null,
                now,
                now);
        activityRepository.save(activity);
        return activity;
    }

    @Transactional
    public Activity update(
            UUID activityId,
            String title,
            String category,
            Instant startAt,
            String location,
            int capacity,
            PolicyType policyType) {
        Activity activity = loadActiveForUpdate(activityId);
        validate(title, category, startAt, location, capacity, policyType, activity.getStartAt());

        long approvedCount = registrationRepository.countApproved(activityId);
        if (!activity.canReduceCapacityTo(capacity, approvedCount)) {
            throw new InvalidActivityException("capacity cannot be less than approved registrations or <= 0");
        }

        boolean hasActiveRegistration = approvedCount > 0
                || !registrationRepository.findPendingByActivity(activityId).isEmpty();
        if (activity.getPolicyType() != policyType && hasActiveRegistration) {
            // D9-01: active registrations only; historical rejected/cancelled records do not block this rule.
            throw new InvalidActivityException("policyType cannot be changed while active registrations exist");
        }

        Instant now = Instant.now();
        activity.updateDetails(
                title.trim(),
                category.trim(),
                startAt,
                location.trim(),
                capacity,
                policyType,
                now);
        activityRepository.save(activity);
        return activity;
    }

    @Transactional
    public void softDelete(UUID activityId) {
        Activity activity = loadActiveForUpdate(activityId);
        List<Registration> pending = registrationRepository.findPendingByActivity(activityId);
        if (!pending.isEmpty()) {
            throw new ActivityNotDeletableException("activity has pending registrations");
        }
        activity.softDelete(Instant.now());
        activityRepository.save(activity);
    }

    @Transactional(readOnly = true)
    public List<Activity> listVisible(String keyword) {
        String normalized = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return activityRepository.findVisible(normalized);
    }

    @Transactional(readOnly = true)
    public Activity getDetail(UUID activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ActivityNotFoundException("activity not found: " + activityId));
        if (activity.isDeleted()) {
            throw new ActivityNotFoundException("activity not found: " + activityId);
        }
        return activity;
    }

    private Activity loadActiveForUpdate(UUID activityId) {
        Activity activity = activityRepository.findByIdForUpdate(activityId)
                .orElseThrow(() -> new ActivityNotFoundException("activity not found: " + activityId));
        if (activity.isDeleted()) {
            throw new ActivityNotFoundException("activity not found: " + activityId);
        }
        return activity;
    }

    private void validate(
            String title,
            String category,
            Instant startAt,
            String location,
            int capacity,
            PolicyType policyType,
            Instant existingStartAt) {
        if (title == null || title.isBlank()) {
            throw new InvalidActivityException("title is required");
        }
        if (category == null || category.isBlank()) {
            throw new InvalidActivityException("category is required");
        }
        if (startAt == null) {
            throw new InvalidActivityException("startAt is required");
        }
        if (startAt.isBefore(Instant.now()) && !startAt.equals(existingStartAt)) {
            throw new InvalidActivityException("startAt must not be in the past");
        }
        if (location == null || location.isBlank()) {
            throw new InvalidActivityException("location is required");
        }
        if (capacity <= 0) {
            throw new InvalidActivityException("capacity must be > 0");
        }
        if (policyType == null) {
            throw new InvalidActivityException("policyType is required");
        }
    }
}
