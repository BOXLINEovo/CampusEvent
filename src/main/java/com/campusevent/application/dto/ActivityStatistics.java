package com.campusevent.application.dto;

import java.util.UUID;

public class ActivityStatistics {

    private final UUID activityId;
    private final long approvedCount;
    private final long pendingCount;
    private final long remainingCapacity;

    public ActivityStatistics(UUID activityId, long approvedCount, long pendingCount, long remainingCapacity) {
        this.activityId = activityId;
        this.approvedCount = approvedCount;
        this.pendingCount = pendingCount;
        this.remainingCapacity = remainingCapacity;
    }

    public UUID getActivityId() {
        return activityId;
    }

    public long getApprovedCount() {
        return approvedCount;
    }

    public long getPendingCount() {
        return pendingCount;
    }

    public long getRemainingCapacity() {
        return remainingCapacity;
    }
}
