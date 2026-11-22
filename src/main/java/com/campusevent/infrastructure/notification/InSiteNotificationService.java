package com.campusevent.infrastructure.notification;

import com.campusevent.application.exception.DuplicateNotificationException;
import com.campusevent.application.port.MessageRepository;
import com.campusevent.application.port.NotificationService;
import com.campusevent.domain.event.RegistrationEvent;
import com.campusevent.domain.event.RegistrationEventType;
import com.campusevent.domain.model.Message;
import com.campusevent.domain.model.NotificationType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * In-site notification observer. Idempotent by sourceEventId + studentId.
 */
@Component
public class InSiteNotificationService implements NotificationService {

    private final MessageRepository messageRepository;

    public InSiteNotificationService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public void onEvent(RegistrationEvent event) {
        if (event.type() == RegistrationEventType.SUBMITTED
                || event.type() == RegistrationEventType.CANCELLED) {
            return;
        }
        if (messageRepository.existsBySourceEventId(event.eventId())) {
            return;
        }

        String activityTitle = event.activityTitle() == null || event.activityTitle().isBlank()
                ? "活动"
                : event.activityTitle();

        NotificationType type;
        String title;
        String content;
        switch (event.type()) {
            case REGISTERED -> {
                type = NotificationType.REGISTRATION_SUCCESS;
                title = "报名成功";
                content = "你已成功报名《" + activityTitle + "》。";
            }
            case APPROVED -> {
                type = NotificationType.REGISTRATION_APPROVED;
                title = "报名已通过";
                content = "你报名的《" + activityTitle + "》已通过审核。";
            }
            case REJECTED -> {
                type = NotificationType.REGISTRATION_REJECTED;
                title = "报名未通过";
                content = "你报名的《" + activityTitle + "》未通过审核。"
                        + (event.reason() == null || event.reason().isBlank()
                        ? ""
                        : "原因：" + event.reason() + "。");
            }
            default -> {
                return;
            }
        }

        Message message = Message.create(
                event.studentId(),
                event.activityId(),
                event.eventId(),
                type,
                title,
                content,
                Instant.now());
        try {
            messageRepository.save(message);
        } catch (DuplicateNotificationException ignored) {
            // At-least-once delivery: duplicate is treated as success.
        }
    }

    @Override
    public List<Message> listByStudent(UUID studentId) {
        return messageRepository.findByStudent(studentId);
    }

    @Override
    public void markRead(UUID messageId, UUID studentId) {
        messageRepository.markRead(messageId, studentId);
    }

    @Override
    public long unreadCount(UUID studentId) {
        return messageRepository.countUnread(studentId);
    }
}
