package com.campusevent.domain.rule;

import com.campusevent.domain.model.Activity;
import com.campusevent.domain.model.RegistrationStatus;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class DefaultCapacityRule implements CapacityRule {

    @Override
    public CapacityDecision checkSubmission(
            Activity activity,
            long approvedCount,
            RegistrationStatus initialStatus) {
        Objects.requireNonNull(activity, "activity");
        Objects.requireNonNull(initialStatus, "initialStatus");
        if (approvedCount < 0) {
            throw new IllegalArgumentException("approvedCount must be >= 0");
        }
        if (approvedCount < activity.getCapacity()) {
            return CapacityDecision.allow();
        }
        if (initialStatus == RegistrationStatus.APPROVED) {
            return CapacityDecision.reject("名额已满");
        }
        return CapacityDecision.allowWithWarning("已满员，批准可能失败");
    }

    @Override
    public CapacityDecision checkApproval(Activity activity, long approvedCount) {
        Objects.requireNonNull(activity, "activity");
        if (approvedCount < 0) {
            throw new IllegalArgumentException("approvedCount must be >= 0");
        }
        if (approvedCount < activity.getCapacity()) {
            return CapacityDecision.allow();
        }
        return CapacityDecision.reject("名额已满");
    }
}
