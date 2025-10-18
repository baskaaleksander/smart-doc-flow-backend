package com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.model.event;

public record UserRegisteredEvent(
        String email,
        String username,
        String password
) {
}
