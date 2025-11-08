package com.baskaaleksander.smartdocflowbackend.modules.auth.application;

import com.baskaaleksander.smartdocflowbackend.common.exception.InvalidResetTokenException;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.exception.WrongPasswordException;
import com.baskaaleksander.smartdocflowbackend.common.logging.LoggingPort;
import com.baskaaleksander.smartdocflowbackend.common.logging.Slf4jLoggingAdapter;
import com.baskaaleksander.smartdocflowbackend.common.util.TokenGenerator;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api.dto.AuthActionResponse;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api.dto.ResetPasswordRequest;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api.dto.UpdatePasswordRequest;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.AuthUser;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.PasswordResetToken;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port.*;
import com.baskaaleksander.smartdocflowbackend.modules.contracts.PasswordResetEvent;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class PasswordChangeApplicationService {

    private final PasswordEncoder passwordEncoder;
    private final AuthEventPublisherPort authEventPublisherPort;
    private final Clock clock;
    private final AuthUserQueryPort authUserQueryPort;
    private final AuthUserCommandPort authUserCommandPort;
    private final PasswordResetTokenCommandPort passwordResetTokenCommandPort;
    private final PasswordResetTokenQueryPort passwordResetTokenQueryPort;
    private final LoggingPort logger;

    @Value("${auth.reset-token.ttl-hours:24}")
    private long ttlHours;

    private void updatePassword(AuthUser user, String newPassword) {
        String password = passwordEncoder.encode(newPassword);
        user.setPassword(password);
        authUserCommandPort.save(user);
    }

    public AuthActionResponse updatePassword(UUID userId, UpdatePasswordRequest passwordRequest) {
        logger.info("PASSWORD_UPDATE START userId=" + Slf4jLoggingAdapter.shortId(userId));

        AuthUser user = authUserQueryPort.findById(userId)
                .orElseThrow(() -> {
                    logger.warn("PASSWORD_UPDATE FAILED reason=user_not_found userId=" + Slf4jLoggingAdapter.shortId(userId));
                    return new ResourceNotFoundException("User not found");
                });

        if (passwordEncoder.matches(passwordRequest.oldPassword(), user.getPassword())) {
            updatePassword(user, passwordRequest.newPassword());
            logger.info("PASSWORD_UPDATE SUCCESS userId=" + Slf4jLoggingAdapter.shortId(user.getId()));
        } else {
            logger.warn("PASSWORD_UPDATE FAILED reason=wrong_old_password userId=" + Slf4jLoggingAdapter.shortId(user.getId()));
            throw new WrongPasswordException("Old password is wrong");
        }

        return new AuthActionResponse(true);
    }

    public AuthActionResponse resetPassword(ResetPasswordRequest changePasswordRequest) {
        String maskedToken = Slf4jLoggingAdapter.maskToken(changePasswordRequest.token());
        logger.info("PASSWORD_RESET START token=" + maskedToken);

        PasswordResetToken resetToken = passwordResetTokenQueryPort.findByToken(changePasswordRequest.token())
                .orElseThrow(() -> {
                    logger.warn("PASSWORD_RESET FAILED reason=token_not_found token=" + maskedToken);
                    return new InvalidResetTokenException("Token not found");
                });

        if (resetToken.isRevoked()) {
            logger.warn("PASSWORD_RESET FAILED reason=token_revoked token=" + maskedToken + " userId=" + Slf4jLoggingAdapter.shortId(resetToken.getUserId()));
            throw new InvalidResetTokenException("Token is revoked");
        }

        if (resetToken.getExpiresAt().isBefore(Instant.now(clock))) {
            logger.warn("PASSWORD_RESET FAILED reason=token_expired token=" + maskedToken + " expiresAt=" + resetToken.getExpiresAt());
            throw new InvalidResetTokenException("Token is expired");
        }

        AuthUser user = authUserQueryPort.findById(resetToken.getUserId())
                .orElseThrow(() -> {
                    logger.warn("PASSWORD_RESET FAILED reason=user_not_found userId=" + Slf4jLoggingAdapter.shortId(resetToken.getUserId()));
                    return new ResourceNotFoundException("User not found");
                });

        updatePassword(user, changePasswordRequest.newPassword());
        resetToken.setRevoked(true);
        passwordResetTokenCommandPort.save(resetToken);

        logger.info("PASSWORD_RESET SUCCESS userId=" + Slf4jLoggingAdapter.shortId(user.getId()) + " token=" + maskedToken);
        return new AuthActionResponse(true);
    }

    public Boolean checkToken(String token) {
        String maskedToken = Slf4jLoggingAdapter.maskToken(token);
        logger.info("PASSWORD_TOKEN_CHECK START token=" + maskedToken);

        PasswordResetToken resetToken = passwordResetTokenQueryPort.findByToken(token)
                .orElseThrow(() -> {
                    logger.warn("PASSWORD_TOKEN_CHECK FAILED reason=token_not_found token=" + maskedToken);
                    return new InvalidResetTokenException("Token not found");
                });

        if (resetToken.isRevoked()) {
            logger.warn("PASSWORD_TOKEN_CHECK FAILED reason=token_revoked token=" + maskedToken);
            throw new InvalidResetTokenException("Token is revoked");
        }

        if (resetToken.getExpiresAt().isBefore(Instant.now(clock))) {
            logger.warn("PASSWORD_TOKEN_CHECK FAILED reason=token_expired token=" + maskedToken + " expiresAt=" + resetToken.getExpiresAt());
            throw new InvalidResetTokenException("Token is expired");
        }

        logger.info("PASSWORD_TOKEN_CHECK SUCCESS token=" + maskedToken + " userId=" + Slf4jLoggingAdapter.shortId(resetToken.getUserId()));
        return true;
    }

    public AuthActionResponse requestPasswordReset(String email) {
        String emailHash = Slf4jLoggingAdapter.hashEmail(email);
        logger.info("PASSWORD_RESET_REQUEST START emailHash=" + emailHash + " ttlHours=" + ttlHours);

        Optional<AuthUser> userOptional = authUserQueryPort.findByEmail(email);

        if (userOptional.isPresent()) {
            AuthUser user = userOptional.get();
            PasswordResetToken passwordResetToken = new PasswordResetToken();
            passwordResetToken.setUserId(user.getId());

            String token = TokenGenerator.generateToken();
            String maskedToken = Slf4jLoggingAdapter.maskToken(token);

            passwordResetToken.setToken(token);
            passwordResetToken.setExpiresAt(Instant.now(clock).plus(Duration.ofHours(ttlHours)));

            passwordResetTokenCommandPort.invalidateAllTokens(user.getId());
            passwordResetTokenCommandPort.save(passwordResetToken);

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    authEventPublisherPort.publish(new PasswordResetEvent(email, token));
                    logger.info("PASSWORD_RESET_REQUEST EVENT_PUBLISHED emailHash=" + emailHash
                            + " userId=" + Slf4jLoggingAdapter.shortId(user.getId())
                            + " token=" + maskedToken
                            + " expiresAt=" + passwordResetToken.getExpiresAt());
                }
            });

            logger.info("PASSWORD_RESET_REQUEST ACCEPTED emailHash=" + emailHash
                    + " userId=" + Slf4jLoggingAdapter.shortId(user.getId())
                    + " token=" + maskedToken);
        } else {
            logger.info("PASSWORD_RESET_REQUEST ACCEPTED emailHash=" + emailHash + " user=unknown");
        }

        return new AuthActionResponse(true);
    }
}