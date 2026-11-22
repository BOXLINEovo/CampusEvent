// Adopted per D10-01; not executed by AI in batch 10.
package com.campusevent.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActivityTimeTest {

    @Test
    void openBeforeStart() {
        Instant now = Instant.now();
        Activity activity = activity(now.plusSeconds(3600), 2);
        assertThat(activity.isOpenForRegistration(now)).isTrue();
    }

    @Test
    void notOpenAtStart() {
        Instant now = Instant.now();
        Activity activity = activity(now, 2);
        assertThat(activity.isOpenForRegistration(now)).isFalse();
        assertThat(activity.hasStarted(now)).isTrue();
    }

    @Test
    void softDeletedNotOpen() {
        Instant now = Instant.now();
        Activity activity = activity(now.plusSeconds(3600), 2);
        activity.softDelete(now);
        assertThat(activity.isDeleted()).isTrue();
        assertThat(activity.isOpenForRegistration(now)).isFalse();
    }

    @Test
    void capacityCannotGoBelowApproved() {
        Instant now = Instant.now();
        Activity activity = activity(now.plusSeconds(3600), 2);
        assertThat(activity.canReduceCapacityTo(2, 2)).isTrue();
        assertThat(activity.canReduceCapacityTo(1, 2)).isFalse();
        assertThat(activity.canReduceCapacityTo(0, 0)).isFalse();
    }

    private Activity activity(Instant startAt, int capacity) {
        Instant now = Instant.now();
        return new Activity(
                UUID.randomUUID(),
                "活动",
                "讲座",
                startAt,
                "地点",
                capacity,
                PolicyType.DIRECT,
                null,
                now,
                now);
    }
}
