package com.campusevent.application.port;

import com.campusevent.domain.model.Message;
import java.util.List;
import java.util.UUID;

public interface MessageRepository {

    void save(Message message);

    List<Message> findByStudent(UUID studentId);

    void markRead(UUID messageId, UUID studentId);

    boolean existsBySourceEventId(UUID eventId);

    long countUnread(UUID studentId);
}
