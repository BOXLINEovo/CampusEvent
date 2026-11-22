package com.campusevent.application.dto;

import com.campusevent.domain.model.Registration;

public record RegistrationResult(Registration registration, String warning) {
}
