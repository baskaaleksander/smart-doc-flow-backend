package com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserLoginRequest {

    @NotBlank
    @Size(min = 3, max = 50)
    private String username;


    @NotBlank @Size(min = 8, max = 72)
    private String password;
}
