package com.campusevent.application.port;

import com.campusevent.domain.event.RegistrationEvent;

/**
 * Outbox publisher port. Implementations must join the caller's transaction.
 */
public interface RegistrationEventPublisher {

    void publish(RegistrationEvent event);
}
