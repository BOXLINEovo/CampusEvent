package com.campusevent.infrastructure.persistence.jpa.repository;

import com.campusevent.infrastructure.persistence.jpa.entity.MessageJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageJpaRepository extends JpaRepository<MessageJpaEntity, UUID> {

    List<MessageJpaEntity> findByStudentIdOrderByCreatedAtDesc(UUID studentId);

    boolean existsBySourceEventId(UUID sourceEventId);

    long countByStudentIdAndReadFalse(UUID studentId);
}
