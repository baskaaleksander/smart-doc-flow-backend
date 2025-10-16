package com.baskaaleksander.smartdocflowbackend.modules.notifications.domain;

public record PasswordResetEvent(String email, String token) {
}
