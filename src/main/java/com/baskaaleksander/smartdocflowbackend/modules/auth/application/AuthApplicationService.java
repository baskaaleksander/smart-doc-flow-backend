package com.baskaaleksander.smartdocflowbackend.modules.auth.application;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceConflictException;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.util.CookieUtil;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api.AuthUserApiMapper;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api.dto.TokenResponse;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api.dto.UserLoginRequest;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api.dto.UserRegisterRequest;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api.dto.UserResponse;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.AuthUser;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.Role;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.Tokens;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port.*;
import com.baskaaleksander.smartdocflowbackend.modules.contracts.UserRegisteredEvent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthApplicationService {

    private final AuthenticationManager authenticationManager;
    private final TokenUtilPort tokenUtil;
    private final PasswordEncoder passwordEncoder;
    private final CookieUtil cookieUtil;
    private final AuthEventPublisherPort authEventPublisherPort;
    private final AuthUserCommandPort authUserCommandPort;
    private final AuthUserQueryPort authUserQueryPort;
    private final RoleQueryPort roleQueryPort;
    private final AuthUserApiMapper mapper;

    public TokenResponse loginUser(UserLoginRequest user) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        user.username(),
                        user.password()
                )
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        Tokens tokens = tokenUtil.issueTokens(userDetails.getUsername());

        return new TokenResponse(tokens.getAccessToken(), tokens.getRefreshToken());
    }

    @Transactional
    public UserResponse registerUser(UserRegisterRequest user) {

        Optional<AuthUser> existingUser = authUserQueryPort.findUserByUsernameWithRoles(user.username());

        if (existingUser.isPresent()) {
            throw new ResourceConflictException("User with " + user.username() + " username already exists");
        }

        Optional<AuthUser> existingUser2 = authUserQueryPort.findByEmail(user.email());

        if (existingUser2.isPresent()) {
            throw new ResourceConflictException("User with " + user.email() + " email already exists");
        }


        String password = generateRandomPassword();
        String encodedPassword = passwordEncoder.encode(password);
        Set<String> roles = roleQueryPort.findAllByRoleIn(user.roles()).stream().map(Role::getRole).collect(Collectors.toSet());

        if (roles.size() != user.roles().size()) {
            throw new ResourceNotFoundException("One or more roles not found");
        }

        AuthUser newUser = new AuthUser();
        newUser.setUsername(user.username());
        newUser.setEmail(user.email());
        newUser.setPassword(encodedPassword);
        newUser.setRoles(roles);
        newUser.setActive(true);

        AuthUser userCreated = authUserCommandPort.save(newUser);

        authEventPublisherPort.publish(new UserRegisteredEvent(userCreated.getEmail(), userCreated.getUsername(), password));

        return mapper.toResponse(userCreated);
    }

    public TokenResponse refreshAccessToken(String refreshToken) {
        Tokens tokens = tokenUtil.refreshAccessToken(refreshToken);
        return new TokenResponse(tokens.getAccessToken(), tokens.getRefreshToken());
    }

    public String logoutUser(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieUtil.parseRefreshTokenCookie(request);
        cookieUtil.clearRefreshTokenCookie(response);
        tokenUtil.invalidateRefreshToken(refreshToken);

        return "Logout successful";
    }

    public UserResponse getMe(UUID userId) {
        return mapper.toResponse(
                authUserQueryPort.findByIdWithRoles(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"))
        );
    }


    private String generateRandomPassword() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@!#$%&";
        String password = RandomStringUtils.random(14, characters);


        String regex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@!#$%&])(?=\\S+$).{8,}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(password);

        if (matcher.matches()) {
            return password;
        } else {
            return generateRandomPassword();
        }
    }

}
