package com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence;

import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.RefreshToken;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port.RefreshTokenCommandPort;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port.RefreshTokenQueryPort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class RefreshTokenJpaAdapter implements RefreshTokenCommandPort, RefreshTokenQueryPort {
    @Override
    public RefreshToken save(RefreshToken token) {
        return null;
    }

    @Override
    public Optional<RefreshToken> findValidToken(String token) {
        return Optional.empty();
    }
}
