package com.campusevent.application.port;

import com.campusevent.domain.model.EmailNotificationV2;
import java.util.List;
import java.util.UUID;

public interface EmailNotificationRepositoryV2 {

    boolean existsBySourceEventIdAndStudentId(UUID sourceEventId, UUID studentId);

    void save(EmailNotificationV2 notification);

    List<EmailNotificationV2> findAll();
}
