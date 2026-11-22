package com.campusevent.application.port;

import com.campusevent.domain.model.Registration;
import com.campusevent.domain.model.RegistrationStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RegistrationRepository {

    Optional<Registration> findById(UUID registrationId);

    void insert(Registration registration);

    boolean updateStatusIfCurrent(
            UUID registrationId,
            RegistrationStatus expected,
            RegistrationStatus target,
            Instant now);

    Optional<Registration> findActiveBy(UUID activityId, UUID studentId);

    long countApproved(UUID activityId);

    List<Registration> findPendingByActivity(UUID activityId);

    List<Registration> findByStudent(UUID studentId);
}
