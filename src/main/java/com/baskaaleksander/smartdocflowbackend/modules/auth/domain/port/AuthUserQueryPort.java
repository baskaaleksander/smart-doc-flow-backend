package com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port;

import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.AuthUser;

import java.util.Optional;
import java.util.UUID;

public interface AuthUserQueryPort {
    Optional<AuthUser> findUserByUsernameWithRoles(String username);
    Optional<AuthUser> findById(UUID id);
    Optional<AuthUser> findByEmail(String email);
}
