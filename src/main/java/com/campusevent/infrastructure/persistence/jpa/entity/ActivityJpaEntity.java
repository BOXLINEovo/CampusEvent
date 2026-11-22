package com.campusevent.infrastructure.persistence.jpa.entity;

import com.campusevent.domain.model.PolicyType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "activity")
public class ActivityJpaEntity {

    @Id
    @Column(name = "activity_id", nullable = false)
    private UUID activityId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "category", nullable = false, length = 60)
    private String category;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "location", nullable = false, length = 200)
    private String location;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_type", nullable = false, length = 20)
    private PolicyType policyType;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ActivityJpaEntity() {
    }

    public ActivityJpaEntity(
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
