package com.campusevent.application.service;

import com.campusevent.application.dto.RegistrationResult;
import com.campusevent.application.exception.ActivityNotFoundException;
import com.campusevent.application.exception.ActivityNotOpenException;
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
import com.campusevent.domain.rule.CapacityRule;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Extension-only decorator: checks grade eligibility before delegating to the unchanged
 * RegistrationService. Existing direct/review flows remain in the original service.
 */
@Service
@Primary
public class GradeAwareRegistrationServiceV2 extends RegistrationService {

    private final ActivityRepository activityRepository;
    private final StudentGradeRepositoryV2 studentGradeRepository;
    private final ActivityGradeRuleRepositoryV2 activityGradeRuleRepository;

    public GradeAwareRegistrationServiceV2(
            ActivityRepository activityRepository,
            RegistrationRepository registrationRepository,
            RegistrationPolicyFactory policyFactory,
            CapacityRule capacityRule,
            RegistrationEventPublisher eventPublisher,
            StudentGradeRepositoryV2 studentGradeRepository,
            ActivityGradeRuleRepositoryV2 activityGradeRuleRepository) {
        super(activityRepository, registrationRepository, policyFactory, capacityRule, eventPublisher);
        this.activityRepository = activityRepository;
        this.studentGradeRepository = studentGradeRepository;
        this.activityGradeRuleRepository = activityGradeRuleRepository;
    }

    @Override
    @Transactional
    public RegistrationResult register(UUID studentId, UUID activityId) {
        Optional<ActivityGradeRuleV2> rule = activityGradeRuleRepository.findByActivityId(activityId);
        if (rule.isEmpty()) {
            return super.register(studentId, activityId);
        }

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ActivityNotFoundException("activity not found: " + activityId));
        if (activity.isDeleted()) {
            throw new ActivityNotOpenException("activity is not open for registration");
        }
        if (activity.getPolicyType() != PolicyType.DIRECT) {
            throw new GradeRestrictionExceptionV2("年级限定规则只适用于直接报名活动");
        }

        OptionalInt grade = studentGradeRepository.findGrade(studentId);
        if (grade.isEmpty()) {
            throw new GradeRestrictionExceptionV2("学生年级信息缺失，无法报名年级限定活动");
        }
        if (!rule.get().allows(grade.getAsInt())) {
            throw new GradeRestrictionExceptionV2(
                    "该活动仅限" + allowedGradesLabel(rule.get()) + "学生报名，当前年级："
                            + gradeLabel(grade.getAsInt()));
        }

        return super.register(studentId, activityId);
    }

    private String allowedGradesLabel(ActivityGradeRuleV2 rule) {
        StringBuilder label = new StringBuilder();
        for (int grade = rule.minGrade(); grade <= rule.maxGrade(); grade++) {
            if (label.length() > 0) {
                label.append("、");
            }
            label.append(gradeLabel(grade));
        }
        return label.toString();
    }

    private String gradeLabel(int grade) {
        return switch (grade) {
            case 1 -> "大一";
            case 2 -> "大二";
            case 3 -> "大三";
            case 4 -> "大四";
            default -> "年级" + grade;
        };
    }
}
