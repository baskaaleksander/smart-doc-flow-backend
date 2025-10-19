package com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor @NoArgsConstructor
@Getter @Setter
public class Role {
    private long id;
    private String description;
    private String role;
}
