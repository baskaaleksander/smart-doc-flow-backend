package com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence;

import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.entity.PasswordResetTokenEntity;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.mapping.PasswordResetTokenPersistenceMapper;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.spring.SpringDataPasswordResetTokenRepository;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.PasswordResetToken;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port.PasswordResetTokenCommandPort;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port.PasswordResetTokenQueryPort;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.spring.SpringDataUserRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class PasswordResetTokenJpaAdapter implements PasswordResetTokenCommandPort, PasswordResetTokenQueryPort {

    private final SpringDataPasswordResetTokenRepository tokenRepo;
    private final SpringDataUserRepository userRepo;
    private final PasswordResetTokenPersistenceMapper mapper;

    public PasswordResetTokenJpaAdapter(
            SpringDataPasswordResetTokenRepository tokenRepo,
            SpringDataUserRepository userRepo,
            PasswordResetTokenPersistenceMapper mapper
    ) {
        this.tokenRepo = tokenRepo;
        this.userRepo = userRepo;
        this.mapper = mapper;
    }
    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        PasswordResetTokenEntity entity = (token.getId() != null) ?
                tokenRepo.findById(token.getId()).orElseGet(() -> {
                    PasswordResetTokenEntity e = new PasswordResetTokenEntity();
                    e.setId(token.getId());
                    return e;
                })
                : new PasswordResetTokenEntity();

        entity.setToken(token.getToken());
        entity.setExpiresAt(token.getExpiresAt());
        entity.setRevoked(token.isRevoked());

        entity.setUser(userRepo.getReferenceById(token.getUserId()));

        PasswordResetTokenEntity saved = tokenRepo.save(entity);

        return mapper.toDomain(saved);
    }

    @Override
    public Integer invalidateAllTokens(UUID userId) {
        return tokenRepo.invalidateAllTokens(userId);
    }

    @Override
    public Optional<PasswordResetToken> findByToken(String token) {
        return tokenRepo.findByToken(token).map(mapper::toDomain);
    }
}
