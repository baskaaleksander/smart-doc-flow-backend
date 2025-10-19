package com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api.dto;


import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

public record UserRegisterRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Email String email,
        @NotEmpty(message = "Roles cannot be empty") Set<String> roles
){

}
