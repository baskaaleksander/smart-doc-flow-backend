package com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.spring;

import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.entity.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataPasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, UUID> {

    @Query("select t from PasswordResetTokenEntity t left join fetch t.user where t.token = :token")
    Optional<PasswordResetTokenEntity> findByToken(String token);

    @Query("select t from PasswordResetTokenEntity t left join fetch t.user where t.user.email = :email and t.revoked = false")
    Optional<PasswordResetTokenEntity> findByUserEmailAndRevokedFalse(String email);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PasswordResetTokenEntity t set t.revoked = true where t.user.id = :userId")
    Integer invalidateAllTokens(UUID userId);
}
