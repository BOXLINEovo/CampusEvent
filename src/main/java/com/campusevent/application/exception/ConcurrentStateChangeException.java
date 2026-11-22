package com.campusevent.application.exception;

public class ConcurrentStateChangeException extends RuntimeException {

    public ConcurrentStateChangeException(String message) {
        super(message);
    }
}
