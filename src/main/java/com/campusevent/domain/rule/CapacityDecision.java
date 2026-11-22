package com.campusevent.domain.rule;

public record CapacityDecision(boolean allowed, boolean warning, String message) {

    public static CapacityDecision allow() {
        return new CapacityDecision(true, false, null);
    }

    public static CapacityDecision allowWithWarning(String message) {
        return new CapacityDecision(true, true, message);
    }

    public static CapacityDecision reject(String message) {
        return new CapacityDecision(false, false, message);
    }
}
