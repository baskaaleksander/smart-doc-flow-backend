package com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor @NoArgsConstructor
@Getter @Setter
public class RefreshToken {
    private String jti;
    private UUID userId;
    private LocalDateTime expiresAt;
    private boolean revoked;
    private String replacedBy;
    private LocalDateTime createdAt;
}
