package com.campusevent.infrastructure.outbox;

import com.campusevent.application.port.OutboxRepository;
import com.campusevent.application.port.RegistrationEventObserver;
import com.campusevent.domain.event.OutboxEvent;
import com.campusevent.domain.event.RegistrationEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Outbox dispatcher: claims PENDING events and delivers them to observers.
 * Each event is processed in its own transaction; delivery failures do not affect registrations.
 */
@Component
public class EventDispatcher {

    private static final int BATCH_SIZE = 20;

    private final OutboxRepository outboxRepository;
    private final List<RegistrationEventObserver> observers;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public EventDispatcher(
            OutboxRepository outboxRepository,
            List<RegistrationEventObserver> observers,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager) {
        this.outboxRepository = outboxRepository;
        this.observers = observers;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Scheduled(fixedDelayString = "${campusevent.outbox.dispatch-delay-ms:5000}")
    public void dispatchPending() {
        List<OutboxEvent> events = outboxRepository.findPending(BATCH_SIZE);
        for (OutboxEvent event : events) {
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    if (!outboxRepository.markProcessing(event.getEventId())) {
                        return;
                    }
                    RegistrationEvent domainEvent;
                    try {
                        domainEvent = objectMapper.readValue(event.getPayload(), RegistrationEvent.class);
                    } catch (JsonProcessingException ex) {
                        throw new IllegalStateException("failed to deserialize registration event", ex);
                    }
                    for (RegistrationEventObserver observer : observers) {
                        observer.onEvent(domainEvent);
                    }
                    outboxRepository.markSent(event.getEventId());
                });
            } catch (Exception ex) {
                Instant nextAttemptAt = Instant.now().plus(backoff(event.getRetryCount()));
                transactionTemplate.executeWithoutResult(status ->
                        outboxRepository.markFailed(
                                event.getEventId(),
                                rootMessage(ex),
                                nextAttemptAt));
            }
        }
    }

    private Duration backoff(int retryCount) {
        return switch (retryCount) {
            case 0 -> Duration.ofMinutes(1);
            case 1 -> Duration.ofMinutes(5);
            case 2 -> Duration.ofMinutes(15);
            case 3 -> Duration.ofHours(1);
            default -> Duration.ofHours(1);
        };
    }

    private String rootMessage(Exception ex) {
        Throwable current = ex;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            message = current.getClass().getSimpleName();
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
