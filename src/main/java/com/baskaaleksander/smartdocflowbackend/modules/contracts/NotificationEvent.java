package com.baskaaleksander.smartdocflowbackend.modules.contracts;

public record NotificationEvent(
        String username,
        String type,
        String message
) {
}
