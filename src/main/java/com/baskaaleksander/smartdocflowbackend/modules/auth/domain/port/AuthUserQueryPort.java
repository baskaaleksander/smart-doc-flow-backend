package com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port;

import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.AuthUser;

import java.util.Optional;

public interface AuthUserQueryPort {
    Optional<AuthUser> findUserByUsernameWithRoles(String username);
}
