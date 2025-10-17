package com.baskaaleksander.smartdocflowbackend.modules.users.adapters.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
public class EditUserAccountAdminRequest {

    @NotBlank @Email
    private String email;

    @NotEmpty
    private Set<String> roles;

    @NotNull
    private Boolean active;
}
