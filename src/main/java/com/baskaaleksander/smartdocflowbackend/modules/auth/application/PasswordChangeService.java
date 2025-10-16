package com.baskaaleksander.smartdocflowbackend.modules.auth.application;

import com.baskaaleksander.smartdocflowbackend.common.exception.InvalidResetTokenException;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.exception.WrongPasswordException;
import com.baskaaleksander.smartdocflowbackend.common.util.TokenGenerator;
import com.baskaaleksander.smartdocflowbackend.modules.auth.api.dto.ChangePasswordRequest;
import com.baskaaleksander.smartdocflowbackend.modules.auth.api.dto.UpdatePasswordRequest;
import com.baskaaleksander.smartdocflowbackend.modules.auth.persistence.PasswordResetToken;
import com.baskaaleksander.smartdocflowbackend.modules.auth.persistence.PasswordResetTokenRepository;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.application.email.PasswordResetEmailTaskPublisher;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.PasswordResetEvent;
import com.baskaaleksander.smartdocflowbackend.modules.users.persistence.User;
import com.baskaaleksander.smartdocflowbackend.modules.users.persistence.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class PasswordChangeService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetEmailTaskPublisher passwordResetEmailTaskPublisher;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public PasswordChangeService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            PasswordResetEmailTaskPublisher passwordResetEmailTaskPublisher, PasswordResetTokenRepository passwordResetTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetEmailTaskPublisher = passwordResetEmailTaskPublisher;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    private String updatePassword(User user, String newPassword) {
        String password = passwordEncoder.encode(newPassword);

        user.setPassword(password);

        userRepository.save(user);

        return "Password changed";

    }


    public String updatePassword(UUID userId, UpdatePasswordRequest passwordRequest) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (passwordEncoder.matches(passwordRequest.oldPassword(), user.getPassword())) {
            return updatePassword(user, passwordRequest.newPassword());
        } else {
            throw new WrongPasswordException("Old password is wrong");
        }

    }

    public String changePassword(ChangePasswordRequest changePasswordRequest) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(changePasswordRequest.token())
                .orElseThrow(() -> new InvalidResetTokenException("Token not found"));

        return updatePassword(resetToken.getUser(), changePasswordRequest.newPassword());
    }

    public Boolean checkToken(String token) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidResetTokenException("Token not found"));

        if (resetToken.isRevoked()) { throw new InvalidResetTokenException("Token is revoked"); }

        if (resetToken.getExpiresAt().isBefore(Instant.now())) { throw new InvalidResetTokenException("Token is expired"); }

        return true;
    }

    public String requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        PasswordResetToken passwordResetToken = new PasswordResetToken();
        passwordResetToken.setUser(user);

        String token = TokenGenerator.generateToken();

        passwordResetToken.setToken(token);
        passwordResetToken.setExpiresAt(Instant.now().plus(Duration.ofHours(24)));

        passwordResetEmailTaskPublisher.enqueue(new PasswordResetEvent(
                email,
                token
        ));

        return "Token generated";
    }
}
