package com.campusevent.domain.exception;

public class InvalidRegistrationStateException extends RuntimeException {

    public InvalidRegistrationStateException(String message) {
        super(message);
    }
}
