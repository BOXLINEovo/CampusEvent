package com.campusevent.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActivityGradeRuleV2Test {

    @Test
    void allowsOnlyGradesInsideRange() {
        ActivityGradeRuleV2 rule = rule(2, 3);

        assertThat(rule.allows(2)).isTrue();
        assertThat(rule.allows(3)).isTrue();
        assertThat(rule.allows(1)).isFalse();
        assertThat(rule.allows(4)).isFalse();
    }

    @Test
    void rejectsInvalidRange() {
        assertThatThrownBy(() -> rule(4, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minGrade");
    }

    @Test
    void rejectsGradeOutsideOneToFour() {
        assertThatThrownBy(() -> rule(0, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("grade");
    }

    private ActivityGradeRuleV2 rule(int min, int max) {
        Instant now = Instant.now();
        return new ActivityGradeRuleV2(UUID.randomUUID(), min, max, now, now);
    }
}
