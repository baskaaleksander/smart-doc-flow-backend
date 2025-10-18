package com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.model.event;

public record PasswordResetEvent(String email, String token) {
}
