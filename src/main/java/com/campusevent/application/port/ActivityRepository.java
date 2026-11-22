package com.campusevent.application.port;

import com.campusevent.domain.model.Activity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivityRepository {

    Optional<Activity> findById(UUID activityId);

    /**
     * Loads the activity with a pessimistic write lock (must be called inside a transaction).
     * Used to serialize capacity checks with registration/approval/update.
     */
    Optional<Activity> findByIdForUpdate(UUID activityId);

    void save(Activity activity);

    List<Activity> findVisible(String keyword);
}
