package com.campusevent.interfaces.web.view;

import com.campusevent.domain.model.Registration;

public class RegistrationView {

    private final Registration registration;
    private final String activityTitle;

    public RegistrationView(Registration registration, String activityTitle) {
        this.registration = registration;
        this.activityTitle = activityTitle;
    }

    public Registration getRegistration() {
        return registration;
    }

    public String getActivityTitle() {
        return activityTitle;
    }
}
