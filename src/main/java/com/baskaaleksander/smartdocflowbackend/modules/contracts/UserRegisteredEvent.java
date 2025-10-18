package com.baskaaleksander.smartdocflowbackend.modules.contracts;

public record UserRegisteredEvent(
        String email,
        String username,
        String password
) {
}
