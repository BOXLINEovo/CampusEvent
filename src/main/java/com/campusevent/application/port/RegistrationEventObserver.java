package com.campusevent.application.port;

import com.campusevent.domain.event.RegistrationEvent;

public interface RegistrationEventObserver {

    void onEvent(RegistrationEvent event);
}
