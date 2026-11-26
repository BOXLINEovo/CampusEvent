package com.campusevent.application.dto;

public record EmailSendResultV2(boolean success, String error) {

    public static EmailSendResultV2 ok() {
        return new EmailSendResultV2(true, null);
    }

    public static EmailSendResultV2 failure(String error) {
        return new EmailSendResultV2(false, error);
    }
}
