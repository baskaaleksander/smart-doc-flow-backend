package com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence;

import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.PasswordResetToken;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port.PasswordResetTokenCommandPort;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port.PasswordResetTokenQueryPort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class PasswordResetTokenJpaAdapter implements PasswordResetTokenCommandPort, PasswordResetTokenQueryPort {
    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        return null;
    }

    @Override
    public Integer invalidateAllTokens(UUID userId) {
        return 0;
    }

    @Override
    public Optional<PasswordResetToken> findByToken(String token) {
        return Optional.empty();
    }
}
