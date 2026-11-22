package com.campusevent.domain.rule;

import com.campusevent.domain.model.Activity;
import com.campusevent.domain.model.RegistrationStatus;

/**
 * Public capacity rule (D4-01=C): submission check + approval hard check.
 * Implementations must be pure: no repository access, no transaction.
 */
public interface CapacityRule {

    CapacityDecision checkSubmission(Activity activity, long approvedCount, RegistrationStatus initialStatus);

    CapacityDecision checkApproval(Activity activity, long approvedCount);
}
