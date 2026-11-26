package com.campusevent.infrastructure.persistence.jpa.repository;

import com.campusevent.infrastructure.persistence.jpa.entity.EmailNotificationJpaEntityV2;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailNotificationJpaRepositoryV2
        extends JpaRepository<EmailNotificationJpaEntityV2, UUID> {

    boolean existsBySourceEventIdAndStudentId(UUID sourceEventId, UUID studentId);

    List<EmailNotificationJpaEntityV2> findAllByOrderByCreatedAtDesc();
}
