package com.campusevent.application.service;

import com.campusevent.application.exception.ActivityNotFoundException;
import com.campusevent.application.exception.InvalidActivityException;
import com.campusevent.application.port.ActivityGradeRuleRepositoryV2;
import com.campusevent.application.port.ActivityRepository;
import com.campusevent.application.port.RegistrationRepository;
import com.campusevent.domain.model.Activity;
import com.campusevent.domain.model.ActivityGradeRuleV2;
import com.campusevent.domain.model.PolicyType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GradeRuleAdminServiceV2 {

    private final ActivityRepository activityRepository;
    private final ActivityGradeRuleRepositoryV2 gradeRuleRepository;
    private final RegistrationRepository registrationRepository;

    public GradeRuleAdminServiceV2(
            ActivityRepository activityRepository,
            ActivityGradeRuleRepositoryV2 gradeRuleRepository,
            RegistrationRepository registrationRepository) {
        this.activityRepository = activityRepository;
        this.gradeRuleRepository = gradeRuleRepository;
        this.registrationRepository = registrationRepository;
    }

    @Transactional(readOnly = true)
    public List<ActivityGradeRuleV2> findAll() {
        return gradeRuleRepository.findAll();
    }

    @Transactional
    public ActivityGradeRuleV2 save(UUID activityId, int minGrade, int maxGrade) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ActivityNotFoundException("activity not found: " + activityId));
        if (activity.isDeleted()) {
            throw new ActivityNotFoundException("activity not found: " + activityId);
        }
        if (activity.getPolicyType() != PolicyType.DIRECT) {
            throw new InvalidActivityException("年级限定规则只适用于直接报名活动");
        }
        if (minGrade < 1 || minGrade > 4 || maxGrade < 1 || maxGrade > 4) {
            throw new InvalidActivityException("年级范围必须在 1—4 之间");
        }
        if (minGrade > maxGrade) {
            throw new InvalidActivityException("起始年级不能大于结束年级");
        }

        Optional<ActivityGradeRuleV2> existing = gradeRuleRepository.findByActivityId(activityId);
        boolean changed = existing.isEmpty()
                || existing.get().minGrade() != minGrade
                || existing.get().maxGrade() != maxGrade;
        if (changed && hasActiveRegistrations(activityId)) {
            throw new InvalidActivityException("已有有效报名，不能修改年级范围");
        }

        Instant now = Instant.now();
        Instant createdAt = existing.map(ActivityGradeRuleV2::createdAt).orElse(now);
        ActivityGradeRuleV2 rule = new ActivityGradeRuleV2(activityId, minGrade, maxGrade, createdAt, now);
        gradeRuleRepository.save(rule);
        return rule;
    }

    private boolean hasActiveRegistrations(UUID activityId) {
        return registrationRepository.countApproved(activityId) > 0
                || !registrationRepository.findPendingByActivity(activityId).isEmpty();
    }
}
