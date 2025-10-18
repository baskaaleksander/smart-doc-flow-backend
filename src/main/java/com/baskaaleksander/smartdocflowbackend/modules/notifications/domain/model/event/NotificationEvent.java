package com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.model.event;

public record NotificationEvent(
        String username,
        String type,
        String message
) {
}
