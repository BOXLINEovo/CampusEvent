package com.campusevent.domain.policy;

import com.campusevent.domain.model.Activity;
import com.campusevent.domain.model.RegistrationStatus;

/**
 * Stable registration strategy contract.
 * It only decides the initial status / approval requirement; capacity is a separate rule.
 */
public interface RegistrationPolicy {

    RegistrationStatus initialStatus(Activity activity);

    boolean requiresApproval();
}
