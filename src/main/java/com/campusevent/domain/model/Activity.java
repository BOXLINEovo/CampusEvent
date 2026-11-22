package com.campusevent.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Activity aggregate root.
 */
public class Activity {

    private final UUID activityId;
    private String title;
    private String category;
    private Instant startAt;
    private String location;
    private int capacity;
    private PolicyType policyType;
    private Instant deletedAt;
    private final Instant createdAt;
    private Instant updatedAt;

    public Activity(
            UUID activityId,
            String title,
            String category,
            Instant startAt,
            String location,
            int capacity,
            PolicyType policyType,
            Instant deletedAt,
            Instant createdAt,
            Instant updatedAt) {
        this.activityId = activityId;
        this.title = title;
        this.category = category;
        this.startAt = startAt;
        this.location = location;
        this.capacity = capacity;
        this.policyType = policyType;
        this.deletedAt = deletedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean hasStarted(Instant now) {
        return !now.isBefore(startAt);
    }

    public boolean isOpenForRegistration(Instant now) {
        return !isDeleted() && now.isBefore(startAt);
    }

    public boolean canReduceCapacityTo(int newCapacity, long approvedCount) {
        return newCapacity > 0 && newCapacity >= approvedCount;
    }

    public void updateDetails(
            String title,
            String category,
            Instant startAt,
            String location,
            int capacity,
            PolicyType policyType,
            Instant now) {
        this.title = title;
        this.category = category;
        this.startAt = startAt;
        this.location = location;
        this.capacity = capacity;
        this.policyType = policyType;
        this.updatedAt = now;
    }

    public void softDelete(Instant now) {
        if (deletedAt == null) {
            deletedAt = now;
        }
        this.updatedAt = now;
    }

    public UUID getActivityId() {
        return activityId;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public String getLocation() {
        return location;
    }

    public int getCapacity() {
        return capacity;
    }

    public PolicyType getPolicyType() {
        return policyType;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
