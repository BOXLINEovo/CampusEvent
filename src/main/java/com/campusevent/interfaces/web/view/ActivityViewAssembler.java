package com.campusevent.interfaces.web.view;

import com.campusevent.application.dto.ActivityStatistics;
import com.campusevent.domain.model.Activity;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Shared assembly of activity views to keep web controllers free of duplicated mapping code.
 */
public final class ActivityViewAssembler {

    private ActivityViewAssembler() {
    }

    public static List<ActivityView> toViews(List<Activity> activities, List<ActivityStatistics> statistics) {
        Map<UUID, ActivityStatistics> byActivityId = statistics.stream()
                .collect(Collectors.toMap(
                        ActivityStatistics::getActivityId,
                        Function.identity(),
                        (left, right) -> left));
        return activities.stream()
                .map(activity -> new ActivityView(activity, byActivityId.get(activity.getActivityId())))
                .toList();
    }

    public static Map<UUID, String> titles(List<Activity> activities) {
        return activities.stream()
                .collect(Collectors.toMap(
                        Activity::getActivityId,
                        Activity::getTitle,
                        (left, right) -> left));
    }
}
