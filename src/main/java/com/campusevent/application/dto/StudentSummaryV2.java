package com.campusevent.application.dto;

import java.util.UUID;

public record StudentSummaryV2(UUID studentId, String name, int grade) {
}
