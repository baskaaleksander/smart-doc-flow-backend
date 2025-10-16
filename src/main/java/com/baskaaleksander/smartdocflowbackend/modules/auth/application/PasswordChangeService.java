package com.baskaaleksander.smartdocflowbackend.modules.auth.application;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.exception.WrongPasswordException;
import com.baskaaleksander.smartdocflowbackend.modules.auth.api.dto.ChangePasswordRequest;
import com.baskaaleksander.smartdocflowbackend.modules.users.persistence.User;
import com.baskaaleksander.smartdocflowbackend.modules.users.persistence.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PasswordChangeService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordChangeService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
}
