package com.campusevent.infrastructure.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.campusevent.application.dto.EmailSendResultV2;
import com.campusevent.application.port.EmailNotificationRepositoryV2;
import com.campusevent.application.port.EmailSenderV2;
import com.campusevent.domain.event.RegistrationEvent;
import com.campusevent.domain.model.Activity;
import com.campusevent.domain.model.EmailNotificationStatusV2;
import com.campusevent.domain.model.EmailNotificationV2;
import com.campusevent.domain.model.PolicyType;
import com.campusevent.domain.model.Registration;
import com.campusevent.domain.model.RegistrationStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailNotificationObserverV2Test {

    @Mock
    private EmailSenderV2 emailSender;

    @Mock
    private EmailNotificationRepositoryV2 emailNotificationRepository;

    private EmailNotificationObserverV2 observer;

    @BeforeEach
    void setUp() {
        observer = new EmailNotificationObserverV2(emailSender, emailNotificationRepository);
    }

    @Test
    void registeredEventCreatesSentEmailRecord() {
        RegistrationEvent event = registeredEvent();
        when(emailNotificationRepository.existsBySourceEventIdAndStudentId(
                event.eventId(), event.studentId())).thenReturn(false);
        when(emailSender.send(any())).thenReturn(EmailSendResultV2.ok());

        observer.onEvent(event);

        ArgumentCaptor<EmailNotificationV2> captor = ArgumentCaptor.forClass(EmailNotificationV2.class);
        verify(emailNotificationRepository).save(captor.capture());
        EmailNotificationV2 saved = captor.getValue();
        assertThat(saved.status()).isEqualTo(EmailNotificationStatusV2.SENT);
        assertThat(saved.recipient()).isEqualTo(event.studentId() + "@campus.example");
        assertThat(saved.subject()).contains("报名成功");
        assertThat(saved.content()).contains("活动");
        assertThat(saved.error()).isNull();
    }

    @Test
    void submittedEventIsSkipped() {
        RegistrationEvent event = submittedEvent();

        observer.onEvent(event);

        verifyNoInteractions(emailSender, emailNotificationRepository);
    }

    @Test
    void senderFailureIsRecordedWithoutThrowing() {
        RegistrationEvent event = registeredEvent();
        when(emailNotificationRepository.existsBySourceEventIdAndStudentId(
                event.eventId(), event.studentId())).thenReturn(false);
        when(emailSender.send(any())).thenThrow(new IllegalStateException("smtp down"));

        observer.onEvent(event);

        ArgumentCaptor<EmailNotificationV2> captor = ArgumentCaptor.forClass(EmailNotificationV2.class);
        verify(emailNotificationRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(EmailNotificationStatusV2.FAILED);
        assertThat(captor.getValue().error()).contains("smtp down");
    }

    @Test
    void duplicateEventIsSkipped() {
        RegistrationEvent event = registeredEvent();
        when(emailNotificationRepository.existsBySourceEventIdAndStudentId(
                event.eventId(), event.studentId())).thenReturn(true);

        observer.onEvent(event);

        verify(emailSender, never()).send(any());
        verify(emailNotificationRepository, never()).save(any());
    }

    private RegistrationEvent registeredEvent() {
        Instant now = Instant.now();
        UUID activityId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Activity activity = new Activity(
                activityId,
                "活动",
                "讲座",
                now.plusSeconds(3600),
                "地点",
                2,
                PolicyType.DIRECT,
                null,
                now,
                now);
        Registration registration = Registration.create(
                activityId, studentId, RegistrationStatus.APPROVED, now);
        return RegistrationEvent.registered(registration, activity, studentId, now);
    }

    private RegistrationEvent submittedEvent() {
        Instant now = Instant.now();
        UUID activityId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Activity activity = new Activity(
                activityId,
                "活动",
                "讲座",
                now.plusSeconds(3600),
                "地点",
                2,
                PolicyType.REVIEW,
                null,
                now,
                now);
        Registration registration = Registration.create(
                activityId, studentId, RegistrationStatus.PENDING, now);
        return RegistrationEvent.submitted(registration, activity, studentId, now);
    }
}
