package com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRegisterRequest {

    @NotBlank @Size(min = 3, max = 50)
    private String username;

    @NotBlank @Email
    private String email;

    @NotEmpty(message = "Roles cannot be empty")
    private Set<String> roles;
}
