package com.campusevent.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Grade eligibility rule attached to an activity.
 * A present rule means the activity is a grade-limited direct-registration activity.
 */
public record ActivityGradeRuleV2(
        UUID activityId,
        int minGrade,
        int maxGrade,
        Instant createdAt,
        Instant updatedAt) {

    public ActivityGradeRuleV2 {
        if (activityId == null) {
            throw new IllegalArgumentException("activityId is required");
        }
        if (minGrade < 1 || minGrade > 4 || maxGrade < 1 || maxGrade > 4) {
            throw new IllegalArgumentException("grade must be between 1 and 4");
        }
        if (minGrade > maxGrade) {
            throw new IllegalArgumentException("minGrade must not be greater than maxGrade");
        }
        if (createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("timestamps are required");
        }
    }

    public boolean allows(int grade) {
        return grade >= minGrade && grade <= maxGrade;
    }
}
