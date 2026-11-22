package com.campusevent.domain.policy;

import com.campusevent.domain.model.Activity;
import com.campusevent.domain.model.RegistrationStatus;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class DirectRegistrationPolicy implements RegistrationPolicy {

    @Override
    public RegistrationStatus initialStatus(Activity activity) {
        Objects.requireNonNull(activity, "activity");
        return RegistrationStatus.APPROVED;
    }

    @Override
    public boolean requiresApproval() {
        return false;
    }
}
