package com.campusevent.interfaces.web.view;

import com.campusevent.application.dto.ActivityStatistics;
import com.campusevent.domain.model.Activity;

public class ActivityView {

    private final Activity activity;
    private final ActivityStatistics statistics;

    public ActivityView(Activity activity, ActivityStatistics statistics) {
        this.activity = activity;
        this.statistics = statistics;
    }

    public Activity getActivity() {
        return activity;
    }

    public ActivityStatistics getStatistics() {
        return statistics;
    }
}
