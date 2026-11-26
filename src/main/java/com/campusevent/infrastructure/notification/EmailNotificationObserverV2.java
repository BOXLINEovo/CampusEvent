package com.campusevent.infrastructure.notification;

import com.campusevent.application.dto.EmailMessageV2;
import com.campusevent.application.dto.EmailSendResultV2;
import com.campusevent.application.exception.DuplicateEmailNotificationExceptionV2;
import com.campusevent.application.port.EmailNotificationRepositoryV2;
import com.campusevent.application.port.EmailSenderV2;
import com.campusevent.application.port.RegistrationEventObserver;
import com.campusevent.domain.event.RegistrationEvent;
import com.campusevent.domain.event.RegistrationEventType;
import com.campusevent.domain.model.EmailNotificationV2;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Email observer for registration outcomes. It is picked up automatically by EventDispatcher.
 * Sending failures are recorded in the email table and logged; they do not roll back the registration.
 */
@Component
public class EmailNotificationObserverV2 implements RegistrationEventObserver {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationObserverV2.class);

    private final EmailSenderV2 emailSender;
    private final EmailNotificationRepositoryV2 emailNotificationRepository;

    public EmailNotificationObserverV2(
            EmailSenderV2 emailSender,
            EmailNotificationRepositoryV2 emailNotificationRepository) {
        this.emailSender = emailSender;
        this.emailNotificationRepository = emailNotificationRepository;
    }

    @Override
    public void onEvent(RegistrationEvent event) {
        if (event.type() != RegistrationEventType.REGISTERED
                && event.type() != RegistrationEventType.APPROVED
                && event.type() != RegistrationEventType.REJECTED) {
            return;
        }
        if (emailNotificationRepository.existsBySourceEventIdAndStudentId(
                event.eventId(), event.studentId())) {
            return;
        }

        String activityTitle = event.activityTitle() == null || event.activityTitle().isBlank()
                ? "活动"
                : event.activityTitle();
        EmailContent content = contentFor(event, activityTitle);
        EmailMessageV2 message = new EmailMessageV2(
                event.studentId() + "@campus.example",
                content.subject(),
                content.body());
        Instant now = Instant.now();

        try {
            EmailSendResultV2 result = emailSender.send(message);
            if (result.success()) {
                saveSafely(EmailNotificationV2.sent(
                        event.eventId(),
                        event.studentId(),
                        message.recipient(),
                        message.subject(),
                        message.content(),
                        now));
            } else {
                log.error("mock email failed: eventId={}, studentId={}, error={}",
                        event.eventId(), event.studentId(), result.error());
                saveSafely(EmailNotificationV2.failed(
                        event.eventId(),
                        event.studentId(),
                        message.recipient(),
                        message.subject(),
                        message.content(),
                        result.error(),
                        now));
            }
        } catch (RuntimeException ex) {
            log.error("mock email failed: eventId={}, studentId={}",
                    event.eventId(), event.studentId(), ex);
            saveSafely(EmailNotificationV2.failed(
                    event.eventId(),
                    event.studentId(),
                    message.recipient(),
                    message.subject(),
                    message.content(),
                    rootMessage(ex),
                    now));
        }
    }

    private void saveSafely(EmailNotificationV2 notification) {
        try {
            emailNotificationRepository.save(notification);
        } catch (DuplicateEmailNotificationExceptionV2 ignored) {
            // At-least-once delivery: an existing row already represents this event/student.
        }
    }

    private EmailContent contentFor(RegistrationEvent event, String activityTitle) {
        return switch (event.type()) {
            case REGISTERED -> new EmailContent(
                    "报名成功：" + activityTitle,
                    "你已成功报名《" + activityTitle + "》。");
            case APPROVED -> new EmailContent(
                    "报名已通过：" + activityTitle,
                    "你报名的《" + activityTitle + "》已通过审核。");
            case REJECTED -> new EmailContent(
                    "报名未通过：" + activityTitle,
                    "你报名的《" + activityTitle + "》未通过审核。"
                            + (event.reason() == null || event.reason().isBlank()
                            ? ""
                            : "原因：" + event.reason() + "。"));
            default -> throw new IllegalStateException("unsupported email event type: " + event.type());
        };
    }

    private String rootMessage(RuntimeException ex) {
        Throwable current = ex;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            return current.getClass().getSimpleName();
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }

    private record EmailContent(String subject, String body) {
    }
}
