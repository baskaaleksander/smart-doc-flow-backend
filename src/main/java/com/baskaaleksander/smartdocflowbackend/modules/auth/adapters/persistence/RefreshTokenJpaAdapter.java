package com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence;

import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.entity.RefreshTokenEntity;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.mapping.RefreshTokenPersistenceMapper;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.spring.SpringDataRefreshTokenRepository;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.RefreshToken;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port.RefreshTokenCommandPort;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port.RefreshTokenQueryPort;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.spring.SpringDataUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RefreshTokenJpaAdapter implements RefreshTokenCommandPort, RefreshTokenQueryPort {

    private final SpringDataRefreshTokenRepository tokenRepo;
    private final SpringDataUserRepository userRepo;
    private final RefreshTokenPersistenceMapper mapper;

    @Override
    @Transactional
    public RefreshToken save(RefreshToken token) {
        RefreshTokenEntity entity = (token.getJti() != null) ?
                tokenRepo.findByJti(token.getJti()).orElseGet(RefreshTokenEntity::new) : new RefreshTokenEntity();

        entity.setJti(token.getJti());
        entity.setExpiresAt(token.getExpiresAt());
        entity.setRevoked(token.isRevoked());

        if (token.getReplacedBy() != null) {
            entity.setReplacedBy(token.getReplacedBy());
        } else {
            entity.setReplacedBy(null);
        }

        entity.setUser(userRepo.getReferenceById(token.getUserId()));

        RefreshTokenEntity saved = tokenRepo.save(entity);

        return mapper.toDomain(saved);
    }

    @Override
    public Optional<RefreshToken> findValidToken(String token) {
        return tokenRepo.findByJtiAndRevokedFalse(token).map(mapper::toDomain);
    }
}
