package com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port;

import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.PasswordResetToken;

import java.util.Optional;

public interface PasswordResetTokenQueryPort {
    Optional<PasswordResetToken> findByToken(String token);
}
