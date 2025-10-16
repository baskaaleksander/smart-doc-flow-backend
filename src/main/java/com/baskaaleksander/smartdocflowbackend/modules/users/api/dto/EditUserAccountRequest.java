package com.baskaaleksander.smartdocflowbackend.modules.users.api.dto;

import jakarta.validation.constraints.Email;

public record EditUserAccountRequest(
        @Email String email
) {
}
