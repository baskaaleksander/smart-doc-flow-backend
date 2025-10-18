package com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api.dto;

import jakarta.validation.constraints.Email;

public record PasswordResetRequest(
        @Email String email
) {
}
