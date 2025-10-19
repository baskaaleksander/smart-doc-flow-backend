package com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port;

import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.RefreshToken;

import java.util.Optional;

public interface RefreshTokenQueryPort {
    Optional<RefreshToken> findValidToken(String token);
}
