package com.baskaaleksander.smartdocflowbackend.modules.auth.application;

import com.baskaaleksander.smartdocflowbackend.common.exception.InvalidResetTokenException;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.exception.WrongPasswordException;
import com.baskaaleksander.smartdocflowbackend.common.util.TokenGenerator;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api.dto.ResetPasswordRequest;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api.dto.UpdatePasswordRequest;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.entity.PasswordResetTokenEntity;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.spring.SpringDataPasswordResetTokenRepository;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.AuthUser;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.PasswordResetToken;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port.*;
import com.baskaaleksander.smartdocflowbackend.modules.contracts.PasswordResetEvent;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.entity.UserEntity;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.spring.SpringDataUserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class PasswordChangeApplicationService {

    private final PasswordEncoder passwordEncoder;

    private final AuthEventPublisherPort authEventPublisherPort;
    private final Clock clock;
    private final AuthUserQueryPort authUserQueryPort;
    private final AuthUserCommandPort authUserCommandPort;
    private final PasswordResetTokenCommandPort passwordResetTokenCommandPort;
    private final PasswordResetTokenQueryPort passwordResetTokenQueryPort;

    @Value("${auth.reset-token.ttl-hours:24}")
    private long ttlHours;

    public PasswordChangeApplicationService(
            PasswordEncoder passwordEncoder,
            AuthEventPublisherPort authEventPublisherPort,
            Clock clock,
            AuthUserQueryPort authUserQueryPort,
            AuthUserCommandPort authUserCommandPort,
            PasswordResetTokenCommandPort passwordResetTokenCommandPort,
            PasswordResetTokenQueryPort passwordResetTokenQueryPort
            ) {
        this.passwordEncoder = passwordEncoder;
        this.authEventPublisherPort = authEventPublisherPort;
        this.clock = clock;
        this.authUserQueryPort = authUserQueryPort;
        this.authUserCommandPort = authUserCommandPort;
        this.passwordResetTokenCommandPort = passwordResetTokenCommandPort;
        this.passwordResetTokenQueryPort = passwordResetTokenQueryPort;
    }

    private void updatePassword(AuthUser user, String newPassword) {
        String password = passwordEncoder.encode(newPassword);

        user.setPassword(password);

        authUserCommandPort.save(user);

    }


    public String updatePassword(UUID userId, UpdatePasswordRequest passwordRequest) {

        AuthUser user = authUserQueryPort.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (passwordEncoder.matches(passwordRequest.oldPassword(), user.getPassword())) {
            updatePassword(user, passwordRequest.newPassword());
        } else {
            throw new WrongPasswordException("Old password is wrong");
        }

        return "Password changed";
    }

    public String resetPassword(ResetPasswordRequest changePasswordRequest) {
        PasswordResetToken resetToken = passwordResetTokenQueryPort.findByToken(changePasswordRequest.token())
                .orElseThrow(() -> new InvalidResetTokenException("Token not found"));

        if (resetToken.isRevoked()) { throw new InvalidResetTokenException("Token is revoked"); }

        if (resetToken.getExpiresAt().isBefore(Instant.now(clock))) { throw new InvalidResetTokenException("Token is expired"); }

        AuthUser user = authUserQueryPort.findById(resetToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        updatePassword(user, changePasswordRequest.newPassword());

        resetToken.setRevoked(true);
        passwordResetTokenCommandPort.save(resetToken);

        return "Password changed";
    }

    public Boolean checkToken(String token) {
        PasswordResetToken resetToken = passwordResetTokenQueryPort.findByToken(token)
                .orElseThrow(() -> new InvalidResetTokenException("Token not found"));

        if (resetToken.isRevoked()) { throw new InvalidResetTokenException("Token is revoked"); }

        if (resetToken.getExpiresAt().isBefore(Instant.now(clock))) { throw new InvalidResetTokenException("Token is expired"); }

        return true;
    }

    public String requestPasswordReset(String email) {
        Optional<AuthUser> userOptional = authUserQueryPort.findByEmail(email);

        if (userOptional.isPresent()) {

            AuthUser user = userOptional.get();

            PasswordResetToken passwordResetToken = new PasswordResetToken();
            passwordResetToken.setUserId(user.getId());

            String token = TokenGenerator.generateToken();

            passwordResetToken.setToken(token);
            passwordResetToken.setExpiresAt(Instant.now(clock).plus(Duration.ofHours(ttlHours)));

            passwordResetTokenCommandPort.invalidateAllTokens(user.getId());

            passwordResetTokenCommandPort.save(passwordResetToken);

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    authEventPublisherPort.publish(new PasswordResetEvent(email, token));
                }
            });
        }

        return "If user exists token will be sent.";
    }
}
