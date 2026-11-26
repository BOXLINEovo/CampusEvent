package com.campusevent.infrastructure.notification;

import com.campusevent.application.dto.EmailMessageV2;
import com.campusevent.application.dto.EmailSendResultV2;
import com.campusevent.application.port.EmailSenderV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Mock sender used by the course project. A real sender can replace this adapter without touching the observer.
 */
@Component
public class MockEmailSenderV2 implements EmailSenderV2 {

    private static final Logger log = LoggerFactory.getLogger(MockEmailSenderV2.class);

    @Override
    public EmailSendResultV2 send(EmailMessageV2 message) {
        log.info("mock email queued: recipient={}, subject={}", message.recipient(), message.subject());
        return EmailSendResultV2.ok();
    }
}
