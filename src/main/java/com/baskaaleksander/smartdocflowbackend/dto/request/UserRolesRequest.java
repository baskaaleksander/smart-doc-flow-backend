package com.baskaaleksander.smartdocflowbackend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRolesRequest {

    @NotEmpty(message = "Roles cannot be empty")
    private Set<String> roles;
}
