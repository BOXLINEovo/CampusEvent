package com.campusevent.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "activity_grade_rule_v2")
public class ActivityGradeRuleJpaEntityV2 {

    @Id
    @Column(name = "activity_id", nullable = false)
    private UUID activityId;

    @Column(name = "min_grade", nullable = false)
    private short minGrade;

    @Column(name = "max_grade", nullable = false)
    private short maxGrade;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ActivityGradeRuleJpaEntityV2() {
    }

    public ActivityGradeRuleJpaEntityV2(
            UUID activityId,
            short minGrade,
            short maxGrade,
            Instant createdAt,
            Instant updatedAt) {
        this.activityId = activityId;
        this.minGrade = minGrade;
        this.maxGrade = maxGrade;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getActivityId() {
        return activityId;
    }

    public short getMinGrade() {
        return minGrade;
    }

    public short getMaxGrade() {
        return maxGrade;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
