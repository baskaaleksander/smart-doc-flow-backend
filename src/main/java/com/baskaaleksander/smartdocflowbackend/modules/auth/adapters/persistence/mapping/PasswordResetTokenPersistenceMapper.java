package com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.mapping;

import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.entity.PasswordResetTokenEntity;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.PasswordResetToken;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetTokenPersistenceMapper {

    public PasswordResetToken toDomain(PasswordResetTokenEntity e) {
        return new PasswordResetToken(
                e.getId(),
                e.getUser().getId(),
                e.getToken(),
                e.getExpiresAt(),
                e.isRevoked(),
                e.getCreatedAt()
        );
    }
}
