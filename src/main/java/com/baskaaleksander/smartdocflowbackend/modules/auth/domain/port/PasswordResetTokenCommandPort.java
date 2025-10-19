package com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port;

import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.PasswordResetToken;

import java.util.UUID;

public interface PasswordResetTokenCommandPort {
    PasswordResetToken save(PasswordResetToken token);
    Integer invalidateAllTokens(UUID userId);
}
