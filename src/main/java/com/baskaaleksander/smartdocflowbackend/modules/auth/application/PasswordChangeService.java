package com.baskaaleksander.smartdocflowbackend.modules.auth.application;

import com.baskaaleksander.smartdocflowbackend.common.exception.InvalidResetTokenException;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.exception.WrongPasswordException;
import com.baskaaleksander.smartdocflowbackend.common.util.TokenGenerator;
import com.baskaaleksander.smartdocflowbackend.modules.auth.api.dto.ResetPasswordRequest;
import com.baskaaleksander.smartdocflowbackend.modules.auth.api.dto.UpdatePasswordRequest;
import com.baskaaleksander.smartdocflowbackend.modules.auth.persistence.PasswordResetToken;
import com.baskaaleksander.smartdocflowbackend.modules.auth.persistence.PasswordResetTokenRepository;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.application.email.PasswordResetEmailTaskPublisher;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.PasswordResetEvent;
import com.baskaaleksander.smartdocflowbackend.modules.users.persistence.User;
import com.baskaaleksander.smartdocflowbackend.modules.users.persistence.UserRepository;
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
public class PasswordChangeService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetEmailTaskPublisher passwordResetEmailTaskPublisher;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final Clock clock;

    @Value("${auth.reset-token.ttl-hours:24}")
    private long ttlHours;

    public PasswordChangeService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            PasswordResetEmailTaskPublisher passwordResetEmailTaskPublisher,
            PasswordResetTokenRepository passwordResetTokenRepository,
            Clock clock
            ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetEmailTaskPublisher = passwordResetEmailTaskPublisher;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.clock = clock;
    }

    private void updatePassword(User user, String newPassword) {
        String password = passwordEncoder.encode(newPassword);

        user.setPassword(password);

        userRepository.save(user);

    }


    public String updatePassword(UUID userId, UpdatePasswordRequest passwordRequest) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (passwordEncoder.matches(passwordRequest.oldPassword(), user.getPassword())) {
            updatePassword(user, passwordRequest.newPassword());
        } else {
            throw new WrongPasswordException("Old password is wrong");
        }

        return "Password changed";
    }

    public String resetPassword(ResetPasswordRequest changePasswordRequest) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(changePasswordRequest.token())
                .orElseThrow(() -> new InvalidResetTokenException("Token not found"));

        if (resetToken.isRevoked()) { throw new InvalidResetTokenException("Token is revoked"); }

        if (resetToken.getExpiresAt().isBefore(Instant.now(clock))) { throw new InvalidResetTokenException("Token is expired"); }

        updatePassword(resetToken.getUser(), changePasswordRequest.newPassword());

        resetToken.setRevoked(true);
        passwordResetTokenRepository.save(resetToken);

        return "Password changed";
    }

    public Boolean checkToken(String token) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidResetTokenException("Token not found"));

        if (resetToken.isRevoked()) { throw new InvalidResetTokenException("Token is revoked"); }

        if (resetToken.getExpiresAt().isBefore(Instant.now(clock))) { throw new InvalidResetTokenException("Token is expired"); }

        return true;
    }

    public String requestPasswordReset(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {

            User user = userOptional.get();

            PasswordResetToken passwordResetToken = new PasswordResetToken();
            passwordResetToken.setUser(user);

            String token = TokenGenerator.generateToken();

            passwordResetToken.setToken(token);
            passwordResetToken.setExpiresAt(Instant.now(clock).plus(Duration.ofHours(ttlHours)));

            passwordResetTokenRepository.invalidateAllTokens(user.getId());

            passwordResetTokenRepository.save(passwordResetToken);

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    passwordResetEmailTaskPublisher.enqueue(new PasswordResetEvent(email, token));
                }
            });
        }

        return "If user exists token will be sent.";
    }
}
