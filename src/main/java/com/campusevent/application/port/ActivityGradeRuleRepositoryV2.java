package com.campusevent.application.port;

import com.campusevent.domain.model.ActivityGradeRuleV2;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivityGradeRuleRepositoryV2 {

    Optional<ActivityGradeRuleV2> findByActivityId(UUID activityId);

    List<ActivityGradeRuleV2> findAll();

    void save(ActivityGradeRuleV2 rule);
}
