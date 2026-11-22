package com.campusevent.application.port;

import com.campusevent.domain.model.Message;
import java.util.List;
import java.util.UUID;

public interface NotificationService extends RegistrationEventObserver {

    List<Message> listByStudent(UUID studentId);

    void markRead(UUID messageId, UUID studentId);

    long unreadCount(UUID studentId);
}
