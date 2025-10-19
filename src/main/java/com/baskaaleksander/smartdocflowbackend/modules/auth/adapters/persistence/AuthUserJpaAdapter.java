package com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence;

import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.AuthUser;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port.AuthUserCommandPort;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port.AuthUserQueryPort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class AuthUserJpaAdapter implements AuthUserQueryPort, AuthUserCommandPort {
    @Override
    public AuthUser save(AuthUser user) {
        return null;
    }

    @Override
    public Optional<AuthUser> findUserByUsernameWithRoles(String username) {
        return Optional.empty();
    }

    @Override
    public Optional<AuthUser> findById(UUID id) {
        return Optional.empty();
    }

    @Override
    public Optional<AuthUser> findByEmail(String email) {
        return Optional.empty();
    }
}
