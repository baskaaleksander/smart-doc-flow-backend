package com.baskaaleksander.smartdocflowbackend.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
public class RefreshToken {

    @Id
    private UUID jti;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    private UUID replacedBy;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
