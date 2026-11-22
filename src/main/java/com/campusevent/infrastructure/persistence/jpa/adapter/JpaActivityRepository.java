package com.campusevent.infrastructure.persistence.jpa.adapter;

import com.campusevent.application.port.ActivityRepository;
import com.campusevent.domain.model.Activity;
import com.campusevent.infrastructure.persistence.jpa.mapper.ActivityMapper;
import com.campusevent.infrastructure.persistence.jpa.repository.ActivityJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaActivityRepository implements ActivityRepository {

    private final ActivityJpaRepository jpaRepository;

    public JpaActivityRepository(ActivityJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Activity> findById(UUID activityId) {
        return jpaRepository.findById(activityId).map(ActivityMapper::toDomain);
    }

    @Override
    public Optional<Activity> findByIdForUpdate(UUID activityId) {
        return jpaRepository.findByIdForUpdate(activityId).map(ActivityMapper::toDomain);
    }

    @Override
    public void save(Activity activity) {
        jpaRepository.save(ActivityMapper.toEntity(activity));
    }

    @Override
    public List<Activity> findVisible(String keyword) {
        String pattern = keyword == null || keyword.isBlank() ? "%" : "%" + keyword.trim() + "%";
        return jpaRepository.findVisible(pattern).stream()
                .map(ActivityMapper::toDomain)
                .toList();
    }
}
