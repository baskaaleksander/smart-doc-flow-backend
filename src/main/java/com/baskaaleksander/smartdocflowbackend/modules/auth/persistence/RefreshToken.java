package com.baskaaleksander.smartdocflowbackend.modules.auth.persistence;

import com.baskaaleksander.smartdocflowbackend.modules.users.persistence.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class RefreshToken {

    @Id
    private String jti;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    private String replacedBy;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
