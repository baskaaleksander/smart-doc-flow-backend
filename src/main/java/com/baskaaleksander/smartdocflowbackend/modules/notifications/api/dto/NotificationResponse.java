package com.baskaaleksander.smartdocflowbackend.modules.notifications.api.dto;

import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.model.NotificationType;

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
