package com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@AllArgsConstructor @NoArgsConstructor
@Getter @Setter
public class Role {
    private long id;
    private String description;
    private String role;
    private Set<UUID> userIds;
}
