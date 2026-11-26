package com.campusevent.interfaces.web.form;

import java.util.UUID;

public class GradeRuleFormV2 {

    private UUID activityId;
    private int minGrade = 2;
    private int maxGrade = 3;

    public UUID getActivityId() {
        return activityId;
    }

    public void setActivityId(UUID activityId) {
        this.activityId = activityId;
    }

    public int getMinGrade() {
        return minGrade;
    }

    public void setMinGrade(int minGrade) {
        this.minGrade = minGrade;
    }

    public int getMaxGrade() {
        return maxGrade;
    }

    public void setMaxGrade(int maxGrade) {
        this.maxGrade = maxGrade;
    }
}
