package com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.mapping;

import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.entity.RefreshTokenEntity;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.RefreshToken;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenPersistenceMapper {

    public RefreshToken toDomain(RefreshTokenEntity e) {
        return new RefreshToken(
                e.getJti(),
                e.getUser().getId(),
                e.getExpiresAt(),
                e.isRevoked(),
                e.getReplacedBy() != null ? e.getReplacedBy() : null,
                e.getCreatedAt()
        );
    }
}
