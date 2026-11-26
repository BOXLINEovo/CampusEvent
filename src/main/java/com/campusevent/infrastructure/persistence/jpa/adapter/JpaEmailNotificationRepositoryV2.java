package com.campusevent.infrastructure.persistence.jpa.adapter;

import com.campusevent.application.exception.DuplicateEmailNotificationExceptionV2;
import com.campusevent.application.port.EmailNotificationRepositoryV2;
import com.campusevent.domain.model.EmailNotificationV2;
import com.campusevent.infrastructure.persistence.jpa.mapper.EmailNotificationMapperV2;
import com.campusevent.infrastructure.persistence.jpa.repository.EmailNotificationJpaRepositoryV2;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class JpaEmailNotificationRepositoryV2 implements EmailNotificationRepositoryV2 {

    private final EmailNotificationJpaRepositoryV2 jpaRepository;

    public JpaEmailNotificationRepositoryV2(EmailNotificationJpaRepositoryV2 jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsBySourceEventIdAndStudentId(UUID sourceEventId, UUID studentId) {
        return jpaRepository.existsBySourceEventIdAndStudentId(sourceEventId, studentId);
    }

    @Override
    public void save(EmailNotificationV2 notification) {
        try {
            jpaRepository.saveAndFlush(EmailNotificationMapperV2.toEntity(notification));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateEmailNotificationExceptionV2("email notification already exists for event");
        }
    }

    @Override
    public List<EmailNotificationV2> findAll() {
        return jpaRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(EmailNotificationMapperV2::toDomain)
                .toList();
    }
}
