package com.baskaaleksander.smartdocflowbackend.service;

import com.baskaaleksander.smartdocflowbackend.dto.request.UserRequest;
import com.baskaaleksander.smartdocflowbackend.dto.response.TokenResponse;
import com.baskaaleksander.smartdocflowbackend.dto.response.UserResponse;
import com.baskaaleksander.smartdocflowbackend.exceptions.ResourceConflictException;
import com.baskaaleksander.smartdocflowbackend.model.Role;
import com.baskaaleksander.smartdocflowbackend.model.User;
import com.baskaaleksander.smartdocflowbackend.repository.RoleRepository;
import com.baskaaleksander.smartdocflowbackend.repository.UserRepository;
import com.baskaaleksander.smartdocflowbackend.security.JwtUtil;
import com.baskaaleksander.smartdocflowbackend.utils.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final CookieUtil cookieUtil;

    @Autowired
    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            RoleRepository roleRepository,
            JwtUtil jwtUtil,
            PasswordEncoder passwordEncoder,
            CookieUtil cookieUtil) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.cookieUtil = cookieUtil;
    }

    public TokenResponse loginUser(UserRequest user) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        user.getUsername(),
                        user.getPassword()
                )
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        return jwtUtil.issueTokens(userDetails.getUsername());
    }

    @Transactional
    public UserResponse registerUser(UserRequest user) {

        Optional<User> existingUser = userRepository.findByUsername(user.getUsername());
        if (existingUser.isPresent()) {
            throw new ResourceConflictException("User with " + user.getUsername() + " username already exists");
        }
        String password = user.getPassword();
        String encodedPassword = passwordEncoder.encode(password);
        Role role = roleRepository.findRoleByRole("ROLE_USER");
        Set<Role> roles = new HashSet<>();
        roles.add(role);

        User newUser = new User();

        newUser.setUsername(user.getUsername());
        newUser.setPassword(encodedPassword);
        newUser.setRoles(roles);

        User userCreated = userRepository.save(newUser);

        return new UserResponse(
                userCreated.getId(),
                userCreated.getUsername(),
                userCreated.getRoles().stream().map(Role::getRole).toList(),
                userCreated.isActive()
        );
    }

    public TokenResponse refreshAccessToken(String refreshToken) {
        return jwtUtil.refreshAccessToken(refreshToken);
    }

    public String logoutUser(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieUtil.parseRefreshTokenCookie(request);
        cookieUtil.clearRefreshTokenCookie(response);
        jwtUtil.invalidateRefreshToken(refreshToken);

        return "Logout successful";
    }


}
