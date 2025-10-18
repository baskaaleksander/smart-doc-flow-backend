package com.baskaaleksander.smartdocflowbackend.modules.auth.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, UUID> {

    @Query("select t from PasswordResetTokenEntity t left join fetch t.user")
    Optional<PasswordResetTokenEntity> findByToken(String token);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PasswordResetTokenEntity t set t.revoked = true where t.user.id = :userId")
    Integer invalidateAllTokens(UUID userId);
}
