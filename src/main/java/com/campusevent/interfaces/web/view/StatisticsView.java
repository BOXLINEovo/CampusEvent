package com.campusevent.interfaces.web.view;

import com.campusevent.application.dto.ActivityStatistics;

public class StatisticsView {

    private final ActivityStatistics statistics;
    private final String activityTitle;

    public StatisticsView(ActivityStatistics statistics, String activityTitle) {
        this.statistics = statistics;
        this.activityTitle = activityTitle;
    }

    public ActivityStatistics getStatistics() {
        return statistics;
    }

    public String getActivityTitle() {
        return activityTitle;
    }
}
