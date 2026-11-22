package com.campusevent.application.exception;

public class OutboxPersistenceException extends RuntimeException {

    public OutboxPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
