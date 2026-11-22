package com.campusevent.application.exception;

public class DuplicateNotificationException extends RuntimeException {

    public DuplicateNotificationException(String message) {
        super(message);
    }
}
