package com.campusevent.infrastructure.persistence.jpa.adapter;

import com.campusevent.application.exception.DuplicateActiveRegistrationException;
import com.campusevent.application.port.RegistrationRepository;
import com.campusevent.domain.model.Registration;
import com.campusevent.domain.model.RegistrationStatus;
import com.campusevent.infrastructure.persistence.jpa.mapper.RegistrationMapper;
import com.campusevent.infrastructure.persistence.jpa.repository.RegistrationJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class JpaRegistrationRepository implements RegistrationRepository {

    private static final List<RegistrationStatus> ACTIVE_STATUSES =
            List.of(RegistrationStatus.PENDING, RegistrationStatus.APPROVED);

    private final RegistrationJpaRepository jpaRepository;

    public JpaRegistrationRepository(RegistrationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Registration> findById(UUID registrationId) {
        return jpaRepository.findById(registrationId).map(RegistrationMapper::toDomain);
    }

    @Override
    public void insert(Registration registration) {
        try {
            jpaRepository.saveAndFlush(RegistrationMapper.toEntity(registration));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateActiveRegistrationException("active registration already exists");
        }
    }

    @Override
    public boolean updateStatusIfCurrent(
            UUID registrationId,
            RegistrationStatus expected,
            RegistrationStatus target,
            Instant now) {
        return jpaRepository.updateStatusIfCurrent(registrationId, expected, target, now) == 1;
    }

    @Override
    public Optional<Registration> findActiveBy(UUID activityId, UUID studentId) {
        return jpaRepository.findActive(activityId, studentId, ACTIVE_STATUSES)
                .map(RegistrationMapper::toDomain);
    }

    @Override
    public long countApproved(UUID activityId) {
        return jpaRepository.countByActivityIdAndStatus(activityId, RegistrationStatus.APPROVED);
    }

    @Override
    public List<Registration> findPendingByActivity(UUID activityId) {
        return jpaRepository.findByActivityIdAndStatusOrderByCreatedAtAsc(activityId, RegistrationStatus.PENDING)
                .stream()
                .map(RegistrationMapper::toDomain)
                .toList();
    }

    @Override
    public List<Registration> findByStudent(UUID studentId) {
        return jpaRepository.findByStudentIdOrderByCreatedAtDesc(studentId).stream()
                .map(RegistrationMapper::toDomain)
                .toList();
    }
}
