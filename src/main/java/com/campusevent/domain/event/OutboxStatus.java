package com.campusevent.domain.event;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    SENT,
    FAILED
}
