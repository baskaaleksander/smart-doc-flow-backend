package com.baskaaleksander.smartdocflowbackend.modules.auth.application;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.exception.WrongPasswordException;
import com.baskaaleksander.smartdocflowbackend.common.util.TokenGenerator;
import com.baskaaleksander.smartdocflowbackend.modules.auth.api.dto.ChangePasswordRequest;
import com.baskaaleksander.smartdocflowbackend.modules.auth.persistence.PasswordResetToken;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.application.email.PasswordResetEmailTaskPublisher;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.PasswordResetEvent;
import com.baskaaleksander.smartdocflowbackend.modules.users.persistence.User;
import com.baskaaleksander.smartdocflowbackend.modules.users.persistence.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PasswordChangeService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetEmailTaskPublisher passwordResetEmailTaskPublisher;

    public PasswordChangeService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            PasswordResetEmailTaskPublisher passwordResetEmailTaskPublisher) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetEmailTaskPublisher = passwordResetEmailTaskPublisher;
    }


    public String updatePassword(UUID userId, ChangePasswordRequest passwordRequest) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (passwordEncoder.matches(passwordRequest.oldPassword(), user.getPassword())) {
            String newPassword = passwordEncoder.encode(passwordRequest.newPassword());

            user.setPassword(newPassword);

            userRepository.save(user);
        } else {
            throw new WrongPasswordException("Old password is wrong");
        }

        return "Password changed";
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
