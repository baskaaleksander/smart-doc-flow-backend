package com.baskaaleksander.smartdocflowbackend.modules.users.adapters.api.dto;

import jakarta.validation.constraints.Email;

public record EditUserAccountRequest(
        @Email String email
) {
}
