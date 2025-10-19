package com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


public record UserLoginRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotNull @Size(min = 8, max = 72) String password
        ) {
}
