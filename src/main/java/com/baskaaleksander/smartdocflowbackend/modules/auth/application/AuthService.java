package com.baskaaleksander.smartdocflowbackend.modules.auth.application;

import com.baskaaleksander.smartdocflowbackend.modules.auth.api.dto.UserRequest;
import com.baskaaleksander.smartdocflowbackend.modules.auth.api.dto.TokenResponse;
import com.baskaaleksander.smartdocflowbackend.modules.auth.api.dto.UserResponse;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceConflictException;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.modules.users.mapping.UserMapper;
import com.baskaaleksander.smartdocflowbackend.modules.auth.persistence.Role;
import com.baskaaleksander.smartdocflowbackend.modules.users.persistence.User;
import com.baskaaleksander.smartdocflowbackend.modules.auth.persistence.RoleRepository;
import com.baskaaleksander.smartdocflowbackend.modules.users.persistence.UserRepository;
import com.baskaaleksander.smartdocflowbackend.common.security.JwtUtil;
import com.baskaaleksander.smartdocflowbackend.common.util.CookieUtil;
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
    private final UserMapper userMapper;

    @Autowired
    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            RoleRepository roleRepository,
            JwtUtil jwtUtil,
            PasswordEncoder passwordEncoder,
            CookieUtil cookieUtil, UserMapper userMapper) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.cookieUtil = cookieUtil;
        this.userMapper = userMapper;
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
        Role role = roleRepository.findRoleByRole("ROLE_USER").orElseThrow(() -> new ResourceNotFoundException("Role not found"));
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

    public UserResponse getMe(UserDetails user) {
        return userMapper.toUserResponse(
                userRepository.findUserByUsernameWithRoles(user.getUsername()).orElseThrow(() -> new ResourceNotFoundException("User not found"))
        );
    }


}
