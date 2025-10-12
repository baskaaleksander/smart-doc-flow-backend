package com.baskaaleksander.smartdocflowbackend.modules.notifications.domain;

public record UserRegisteredEvent(
        String email,
        String password
) {
}
