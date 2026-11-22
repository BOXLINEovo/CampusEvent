package com.campusevent.interfaces.web.form;

import com.campusevent.domain.model.Activity;
import com.campusevent.domain.model.PolicyType;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.format.annotation.DateTimeFormat;

public class ActivityForm {

    private String title;
    private String category;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startAt;

    private String location;
    private int capacity;
    private PolicyType policyType = PolicyType.DIRECT;

    public static ActivityForm from(Activity activity) {
        ActivityForm form = new ActivityForm();
        form.title = activity.getTitle();
        form.category = activity.getCategory();
        form.startAt = LocalDateTime.ofInstant(activity.getStartAt(), ZoneId.systemDefault());
        form.location = activity.getLocation();
        form.capacity = activity.getCapacity();
        form.policyType = activity.getPolicyType();
        return form;
    }

    public Instant startAtInstant() {
        return startAt == null ? null : startAt.atZone(ZoneId.systemDefault()).toInstant();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public PolicyType getPolicyType() {
        return policyType;
    }

    public void setPolicyType(PolicyType policyType) {
        this.policyType = policyType;
    }
}
