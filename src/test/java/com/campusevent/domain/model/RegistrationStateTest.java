// Adopted per D10-01; not executed by AI in batch 10.
package com.campusevent.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusevent.domain.exception.InvalidRegistrationStateException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RegistrationStateTest {

    @Test
    void approveMovesPendingToApproved() {
        Registration registration = pending();
        registration.approve(Instant.now());
        assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.APPROVED);
    }

    @Test
    void rejectMovesPendingToRejected() {
        Registration registration = pending();
        registration.reject(Instant.now(), "名额已满");
        assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.REJECTED);
    }

    @Test
    void cancelMovesPendingToCancelled() {
        Registration registration = pending();
        registration.cancel(Instant.now());
        assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.CANCELLED);
    }

    @Test
    void cancelMovesApprovedToCancelled() {
        Registration registration = pending();
        registration.approve(Instant.now());
        registration.cancel(Instant.now());
        assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.CANCELLED);
    }

    @Test
    void approvedCannotBeRejected() {
        Registration registration = pending();
        registration.approve(Instant.now());
        assertThatThrownBy(() -> registration.reject(Instant.now(), "x"))
                .isInstanceOf(InvalidRegistrationStateException.class);
    }

    @Test
    void terminalCannotBeApproved() {
        Registration registration = pending();
        registration.cancel(Instant.now());
        assertThatThrownBy(() -> registration.approve(Instant.now()))
                .isInstanceOf(InvalidRegistrationStateException.class);
    }

    private Registration pending() {
        Instant now = Instant.now();
        return Registration.create(UUID.randomUUID(), UUID.randomUUID(), RegistrationStatus.PENDING, now);
    }
}
