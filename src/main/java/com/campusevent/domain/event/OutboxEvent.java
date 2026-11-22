package com.campusevent.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Transactional outbox record (skeleton).
 * payload stores the serialized RegistrationEvent.
 */
public class OutboxEvent {

    private final UUID eventId;
    private final RegistrationEventType type;
    private final String payload;
    private OutboxStatus status;
    private int retryCount;
    private Instant nextAttemptAt;
    private String lastError;
    private final Instant createdAt;
    private Instant updatedAt;

    public OutboxEvent(
            UUID eventId,
            RegistrationEventType type,
            String payload,
            OutboxStatus status,
            int retryCount,
            Instant nextAttemptAt,
            String lastError,
            Instant createdAt,
            Instant updatedAt) {
        this.eventId = eventId;
        this.type = type;
        this.payload = payload;
        this.status = status;
        this.retryCount = retryCount;
        this.nextAttemptAt = nextAttemptAt;
        this.lastError = lastError;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getEventId() {
        return eventId;
    }

    public RegistrationEventType getType() {
        return type;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
