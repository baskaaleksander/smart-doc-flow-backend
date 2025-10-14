package com.baskaaleksander.smartdocflowbackend.modules.notifications.domain;

public record NotificationEvent(
        String username,
        String type,
        String message
) {
}
