package com.campusevent.infrastructure.persistence.jpa.adapter;

import com.campusevent.application.exception.DuplicateNotificationException;
import com.campusevent.application.exception.MessageNotFoundException;
import com.campusevent.application.exception.NotMessageOwnerException;
import com.campusevent.application.port.MessageRepository;
import com.campusevent.domain.model.Message;
import com.campusevent.infrastructure.persistence.jpa.entity.MessageJpaEntity;
import com.campusevent.infrastructure.persistence.jpa.mapper.MessageMapper;
import com.campusevent.infrastructure.persistence.jpa.repository.MessageJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class JpaMessageRepository implements MessageRepository {

    private final MessageJpaRepository jpaRepository;

    public JpaMessageRepository(MessageJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(Message message) {
        try {
            jpaRepository.saveAndFlush(MessageMapper.toEntity(message));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateNotificationException("notification already exists for event");
        }
    }

    @Override
    public List<Message> findByStudent(UUID studentId) {
        return jpaRepository.findByStudentIdOrderByCreatedAtDesc(studentId).stream()
                .map(MessageMapper::toDomain)
                .toList();
    }

    @Override
    public void markRead(UUID messageId, UUID studentId) {
        MessageJpaEntity entity = jpaRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException("message not found: " + messageId));
        if (!entity.getStudentId().equals(studentId)) {
            throw new NotMessageOwnerException("message does not belong to the student");
        }
        if (!entity.isRead()) {
            entity.setRead(true);
            entity.setReadAt(Instant.now());
            jpaRepository.save(entity);
        }
    }

    @Override
    public boolean existsBySourceEventId(UUID eventId) {
        return jpaRepository.existsBySourceEventId(eventId);
    }

    @Override
    public long countUnread(UUID studentId) {
        return jpaRepository.countByStudentIdAndReadFalse(studentId);
    }
}
