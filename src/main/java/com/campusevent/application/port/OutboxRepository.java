package com.campusevent.application.port;

import com.campusevent.domain.event.OutboxEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxRepository {

    void save(OutboxEvent event);

    List<OutboxEvent> findPending(int limit);

    boolean markProcessing(UUID eventId);

    void markSent(UUID eventId);

    void markFailed(UUID eventId, String error, Instant nextAttemptAt);
}
