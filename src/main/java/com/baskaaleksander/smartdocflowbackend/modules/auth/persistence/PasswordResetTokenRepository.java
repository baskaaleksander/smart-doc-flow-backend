package com.baskaaleksander.smartdocflowbackend.modules.auth.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    @Query("select t from PasswordResetToken t left join fetch t.user")
    Optional<PasswordResetToken> findByToken(String token);
}
