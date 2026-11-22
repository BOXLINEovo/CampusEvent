// Adopted per D10-01; not executed by AI in batch 10.
package com.campusevent.domain.rule;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusevent.domain.model.Activity;
import com.campusevent.domain.model.PolicyType;
import com.campusevent.domain.model.RegistrationStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DefaultCapacityRuleTest {

    private final DefaultCapacityRule rule = new DefaultCapacityRule();

    @Test
    void submissionAllowsWhenNotFull() {
        CapacityDecision decision = rule.checkSubmission(activity(2), 1, RegistrationStatus.APPROVED);
        assertThat(decision.allowed()).isTrue();
        assertThat(decision.warning()).isFalse();
    }

    @Test
    void directSubmissionRejectsWhenFull() {
        CapacityDecision decision = rule.checkSubmission(activity(1), 1, RegistrationStatus.APPROVED);
        assertThat(decision.allowed()).isFalse();
    }

    @Test
    void reviewSubmissionWarnsWhenFull() {
        CapacityDecision decision = rule.checkSubmission(activity(1), 1, RegistrationStatus.PENDING);
        assertThat(decision.allowed()).isTrue();
        assertThat(decision.warning()).isTrue();
    }

    @Test
    void approvalRejectsWhenFull() {
        CapacityDecision decision = rule.checkApproval(activity(1), 1);
        assertThat(decision.allowed()).isFalse();
    }

    private Activity activity(int capacity) {
        Instant now = Instant.now();
        return new Activity(
                UUID.randomUUID(),
                "活动",
                "讲座",
                now.plusSeconds(3600),
                "地点",
                capacity,
                PolicyType.DIRECT,
                null,
                now,
                now);
    }
}
