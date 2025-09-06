package com.baskaaleksander.smartdocflowbackend.service;

import com.baskaaleksander.smartdocflowbackend.dto.request.UserRequest;
import com.baskaaleksander.smartdocflowbackend.dto.response.UserLoginResponse;
import com.baskaaleksander.smartdocflowbackend.exceptions.ResourceConflictException;
import com.baskaaleksander.smartdocflowbackend.model.User;
import com.baskaaleksander.smartdocflowbackend.repository.UserRepository;
import com.baskaaleksander.smartdocflowbackend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthSevice {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthSevice(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            JwtUtil jwtUtil,
            PasswordEncoder passwordEncoder
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    public UserLoginResponse loginUser(UserRequest user) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        user.getUsername(),
                        user.getPassword()
                )
        );


        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String accessToken = jwtUtil.generateAccessToken(userDetails.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(userDetails.getUsername());

        return new UserLoginResponse(accessToken, refreshToken);
    }

    public String registerUser(UserRequest user) {

        Optional<User> existingUser = userRepository.findByUsername(user.getUsername());
        if (existingUser.isPresent()) {
            throw new ResourceConflictException("User with " + user.getUsername() + " username already exists");
        }
        String password = user.getPassword();
        String encodedPassword = passwordEncoder.encode(password);

        User newUser = new User(
                user.getUsername(),
                encodedPassword
        );

        User userCreated = userRepository.save(newUser);
        System.out.println(userCreated);

        return "User registered";
    }

    public String refreshAccessToken(String refreshToken) {
        return jwtUtil.refreshAccessToken(refreshToken);
    }
}
