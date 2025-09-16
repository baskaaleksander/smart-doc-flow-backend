package com.baskaaleksander.smartdocflowbackend.dto.response;

import com.baskaaleksander.smartdocflowbackend.enums.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String username,
        NotificationType type,
        String message,
        boolean read,
        Instant createdAt
) {
}
