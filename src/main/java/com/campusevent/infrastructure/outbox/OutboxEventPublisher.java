package com.campusevent.infrastructure.outbox;

import com.campusevent.application.exception.OutboxPersistenceException;
import com.campusevent.application.port.OutboxRepository;
import com.campusevent.application.port.RegistrationEventPublisher;
import com.campusevent.domain.event.OutboxEvent;
import com.campusevent.domain.event.OutboxStatus;
import com.campusevent.domain.event.RegistrationEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes the domain event into the outbox in the caller's business transaction.
 */
@Component
public class OutboxEventPublisher implements RegistrationEventPublisher {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxEventPublisher(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(RegistrationEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            Instant now = Instant.now();
            OutboxEvent outboxEvent = new OutboxEvent(
                    event.eventId(),
                    event.type(),
                    payload,
                    OutboxStatus.PENDING,
                    0,
                    now,
                    null,
                    now,
                    now);
            outboxRepository.save(outboxEvent);
        } catch (JsonProcessingException ex) {
            throw new OutboxPersistenceException("failed to serialize registration event", ex);
        }
    }
}
