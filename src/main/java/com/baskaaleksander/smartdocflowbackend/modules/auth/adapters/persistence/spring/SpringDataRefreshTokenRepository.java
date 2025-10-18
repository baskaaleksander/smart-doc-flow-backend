package com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.spring;

import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataRefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    Optional<RefreshTokenEntity> findByJtiAndRevokedFalse(String jti);
}
