package com.campusevent.application.exception;

public class DuplicateActiveRegistrationException extends RuntimeException {

    public DuplicateActiveRegistrationException(String message) {
        super(message);
    }
}
