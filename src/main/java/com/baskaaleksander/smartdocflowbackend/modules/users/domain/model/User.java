package com.baskaaleksander.smartdocflowbackend.modules.users.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@AllArgsConstructor @NoArgsConstructor
@Getter @Setter
public class User {
    private UUID id;
    private String username;
    private String email;
    private String password;
    private Set<String> roles;
    private boolean active;
    private Set<UUID> documentIds;
    private Instant createdAt;
}
