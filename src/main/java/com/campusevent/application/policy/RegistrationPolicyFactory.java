package com.campusevent.application.policy;

import com.campusevent.application.exception.PolicyNotFoundException;
import com.campusevent.domain.model.PolicyType;
import com.campusevent.domain.policy.DirectRegistrationPolicy;
import com.campusevent.domain.policy.RegistrationPolicy;
import com.campusevent.domain.policy.ReviewRegistrationPolicy;
import org.springframework.stereotype.Component;

@Component
public class RegistrationPolicyFactory {

    private final DirectRegistrationPolicy directPolicy;
    private final ReviewRegistrationPolicy reviewPolicy;

    public RegistrationPolicyFactory(
            DirectRegistrationPolicy directPolicy,
            ReviewRegistrationPolicy reviewPolicy) {
        this.directPolicy = directPolicy;
        this.reviewPolicy = reviewPolicy;
    }

    public RegistrationPolicy create(PolicyType type) {
        if (type == null) {
            throw new PolicyNotFoundException("policyType is null");
        }
        return switch (type) {
            case DIRECT -> directPolicy;
            case REVIEW -> reviewPolicy;
        };
    }
}
