package com.campusevent.application.dto;

import com.campusevent.domain.model.Activity;
import com.campusevent.domain.model.ActivityGradeRuleV2;

public record GradeRuleRowV2(Activity activity, ActivityGradeRuleV2 rule) {
}
