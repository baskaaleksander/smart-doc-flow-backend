package com.baskaaleksander.smartdocflowbackend.modules.contracts;

public record PasswordResetEvent(String email, String token) {
}
