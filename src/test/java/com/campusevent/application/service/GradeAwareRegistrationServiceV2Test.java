package com.campusevent.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusevent.application.dto.RegistrationResult;
import com.campusevent.application.exception.GradeRestrictionExceptionV2;
import com.campusevent.application.policy.RegistrationPolicyFactory;
import com.campusevent.application.port.ActivityGradeRuleRepositoryV2;
import com.campusevent.application.port.ActivityRepository;
import com.campusevent.application.port.RegistrationEventPublisher;
import com.campusevent.application.port.RegistrationRepository;
import com.campusevent.application.port.StudentGradeRepositoryV2;
import com.campusevent.domain.model.Activity;
import com.campusevent.domain.model.ActivityGradeRuleV2;
import com.campusevent.domain.model.PolicyType;
import com.campusevent.domain.model.Registration;
import com.campusevent.domain.model.RegistrationStatus;
import com.campusevent.domain.policy.DirectRegistrationPolicy;
import com.campusevent.domain.rule.DefaultCapacityRule;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GradeAwareRegistrationServiceV2Test {

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private RegistrationPolicyFactory policyFactory;

    @Mock
    private RegistrationEventPublisher eventPublisher;

    @Mock
    private StudentGradeRepositoryV2 studentGradeRepository;

    @Mock
    private ActivityGradeRuleRepositoryV2 activityGradeRuleRepository;

    private GradeAwareRegistrationServiceV2 service;

    @BeforeEach
    void setUp() {
        service = new GradeAwareRegistrationServiceV2(
                activityRepository,
                registrationRepository,
                policyFactory,
                new DefaultCapacityRule(),
                eventPublisher,
                studentGradeRepository,
                activityGradeRuleRepository);
    }

    @Test
    void gradeOneIsRejectedBeforeRegistrationInsert() {
        Activity activity = activity();
        when(activityGradeRuleRepository.findByActivityId(activity.getActivityId()))
                .thenReturn(Optional.of(rule(2, 3)));
        when(activityRepository.findById(activity.getActivityId())).thenReturn(Optional.of(activity));
        when(studentGradeRepository.findGrade(any())).thenReturn(OptionalInt.of(1));

        assertThatThrownBy(() -> service.register(UUID.randomUUID(), activity.getActivityId()))
                .isInstanceOf(GradeRestrictionExceptionV2.class)
                .hasMessageContaining("大二")
                .hasMessageContaining("大一");

        verify(registrationRepository, never()).insert(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void gradeTwoPassesAndKeepsOriginalApprovedFlow() {
        Activity activity = activity();
        when(activityGradeRuleRepository.findByActivityId(activity.getActivityId()))
                .thenReturn(Optional.of(rule(2, 3)));
        when(activityRepository.findById(activity.getActivityId())).thenReturn(Optional.of(activity));
        when(studentGradeRepository.findGrade(any())).thenReturn(OptionalInt.of(2));
        when(activityRepository.findByIdForUpdate(activity.getActivityId())).thenReturn(Optional.of(activity));
        when(registrationRepository.findActiveBy(eq(activity.getActivityId()), any())).thenReturn(Optional.empty());
        when(registrationRepository.countApproved(activity.getActivityId())).thenReturn(0L);
        when(policyFactory.create(PolicyType.DIRECT)).thenReturn(new DirectRegistrationPolicy());

        RegistrationResult result = service.register(UUID.randomUUID(), activity.getActivityId());

        assertThat(result.registration().getStatus()).isEqualTo(RegistrationStatus.APPROVED);
        verify(registrationRepository).insert(any(Registration.class));
        verify(eventPublisher).publish(any());
    }

    @Test
    void noRuleLeavesOriginalFlowUntouched() {
        Activity activity = activity();
        when(activityGradeRuleRepository.findByActivityId(activity.getActivityId()))
                .thenReturn(Optional.empty());
        when(activityRepository.findByIdForUpdate(activity.getActivityId())).thenReturn(Optional.of(activity));
        when(registrationRepository.findActiveBy(eq(activity.getActivityId()), any())).thenReturn(Optional.empty());
        when(registrationRepository.countApproved(activity.getActivityId())).thenReturn(0L);
        when(policyFactory.create(PolicyType.DIRECT)).thenReturn(new DirectRegistrationPolicy());

        RegistrationResult result = service.register(UUID.randomUUID(), activity.getActivityId());

        assertThat(result.registration().getStatus()).isEqualTo(RegistrationStatus.APPROVED);
        verify(studentGradeRepository, never()).findGrade(any());
    }

    private Activity activity() {
        Instant now = Instant.now();
        return new Activity(
                UUID.randomUUID(),
                "活动",
                "讲座",
                now.plusSeconds(3600),
                "地点",
                2,
                PolicyType.DIRECT,
                null,
                now,
                now);
    }

    private ActivityGradeRuleV2 rule(int min, int max) {
        Instant now = Instant.now();
        return new ActivityGradeRuleV2(UUID.randomUUID(), min, max, now, now);
    }
}
