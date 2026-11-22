package com.campusevent.application.service;

import com.campusevent.application.dto.ActivityStatistics;
import com.campusevent.application.exception.ActivityNotFoundException;
import com.campusevent.application.port.ActivityRepository;
import com.campusevent.application.port.RegistrationRepository;
import com.campusevent.domain.model.Activity;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only statistics use cases (P2-F08).
 */
@Service
public class StatisticsService {

    private final ActivityRepository activityRepository;
    private final RegistrationRepository registrationRepository;

    public StatisticsService(ActivityRepository activityRepository, RegistrationRepository registrationRepository) {
        this.activityRepository = activityRepository;
        this.registrationRepository = registrationRepository;
    }

    @Transactional(readOnly = true)
    public ActivityStatistics getActivityStatistics(UUID activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ActivityNotFoundException("activity not found: " + activityId));
        return statisticsOf(activity);
    }

    @Transactional(readOnly = true)
    public List<ActivityStatistics> getAll() {
        return activityRepository.findVisible(null).stream()
                .map(this::statisticsOf)
                .toList();
    }

    private ActivityStatistics statisticsOf(Activity activity) {
        long approved = registrationRepository.countApproved(activity.getActivityId());
        long pending = registrationRepository.findPendingByActivity(activity.getActivityId()).size();
        long remaining = Math.max(0, activity.getCapacity() - approved);
        return new ActivityStatistics(activity.getActivityId(), approved, pending, remaining);
    }
}
