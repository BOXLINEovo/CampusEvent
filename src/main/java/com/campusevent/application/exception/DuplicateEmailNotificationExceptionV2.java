package com.campusevent.application.exception;

public class DuplicateEmailNotificationExceptionV2 extends RuntimeException {

    public DuplicateEmailNotificationExceptionV2(String message) {
        super(message);
    }
}
