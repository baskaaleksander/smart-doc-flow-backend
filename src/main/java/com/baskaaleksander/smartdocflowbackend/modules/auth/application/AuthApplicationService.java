package com.baskaaleksander.smartdocflowbackend.modules.auth.application;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceConflictException;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.logging.LoggingPort;
import com.baskaaleksander.smartdocflowbackend.common.logging.Slf4jLoggingAdapter;
import com.baskaaleksander.smartdocflowbackend.common.security.CustomUserDetails;
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
    private final LoggingPort logger;

    public TokenResponse loginUser(UserLoginRequest user) {
        logger.info("LOGIN START username=" + user.username());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.username(), user.password())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Tokens tokens = tokenUtil.issueTokens(userDetails.getUsername());

        logger.info("LOGIN SUCCESS userId=" + Slf4jLoggingAdapter.shortId(userDetails.getId())
                + " username=" + userDetails.getUsername());
        return new TokenResponse(tokens.getAccessToken(), tokens.getRefreshToken());
    }

    @Transactional
    public UserResponse registerUser(UserRegisterRequest user) {
        String emailHash = Slf4jLoggingAdapter.hashEmail(user.email());
        logger.info("REGISTER START username=" + user.username()
                + " emailHash=" + emailHash
                + " rolesRequested=" + (user.roles() == null ? 0 : user.roles().size()));

        Optional<AuthUser> existingByUsername = authUserQueryPort.findUserByUsernameWithRoles(user.username());
        if (existingByUsername.isPresent()) {
            logger.warn("REGISTER FAILED reason=username_conflict username=" + user.username());
            throw new ResourceConflictException("User with that username already exists");
        }

        Optional<AuthUser> existingByEmail = authUserQueryPort.findByEmail(user.email());
        if (existingByEmail.isPresent()) {
            logger.warn("REGISTER FAILED reason=email_conflict emailHash=" + emailHash);
            throw new ResourceConflictException("User with that email already exists");
        }

        String password = generateRandomPassword();
        String encodedPassword = passwordEncoder.encode(password);

        Set<String> roles = roleQueryPort.findAllByRoleIn(user.roles()).stream()
                .map(Role::getRole)
                .collect(Collectors.toSet());
        if (roles.size() != user.roles().size()) {
            logger.warn("REGISTER FAILED reason=role_not_found requested=" + user.roles().size() + " found=" + roles.size());
            throw new ResourceNotFoundException("One or more roles not found");
        }

        AuthUser newUser = new AuthUser();
        newUser.setUsername(user.username());
        newUser.setEmail(user.email());
        newUser.setPassword(encodedPassword);
        newUser.setRoles(roles);
        newUser.setActive(true);

        AuthUser created = authUserCommandPort.save(newUser);
        logger.info("REGISTER DB_SAVE_SUCCESS userId=" + Slf4jLoggingAdapter.shortId(created.getId())
                + " roles=" + roles.size());

        authEventPublisherPort.publish(new UserRegisteredEvent(created.getEmail(), created.getUsername(), password));
        logger.info("REGISTER EVENT_PUBLISHED userId=" + Slf4jLoggingAdapter.shortId(created.getId())
                + " emailHash=" + emailHash);

        logger.info("REGISTER SUCCESS userId=" + Slf4jLoggingAdapter.shortId(created.getId()));
        return mapper.toResponse(created);
    }

    public TokenResponse refreshAccessToken(String refreshToken) {
        logger.info("TOKEN_REFRESH START");
        Tokens tokens = tokenUtil.refreshAccessToken(refreshToken);
        logger.info("TOKEN_REFRESH SUCCESS");
        return new TokenResponse(tokens.getAccessToken(), tokens.getRefreshToken());
    }

    public String logoutUser(HttpServletRequest request, HttpServletResponse response) {
        logger.info("LOGOUT START");
        String refreshToken = cookieUtil.parseRefreshTokenCookie(request);
        cookieUtil.clearRefreshTokenCookie(response);
        tokenUtil.invalidateRefreshToken(refreshToken);
        logger.info("LOGOUT SUCCESS");
        return "Logout successful";
    }

    public UserResponse getMe(UUID userId) {
        logger.info("ME_GET START userId=" + Slf4jLoggingAdapter.shortId(userId));
        UserResponse resp = mapper.toResponse(
                authUserQueryPort.findByIdWithRoles(userId)
                        .orElseThrow(() -> {
                            logger.warn("ME_GET FAILED reason=not_found userId=" + Slf4jLoggingAdapter.shortId(userId));
                            return new ResourceNotFoundException("User not found");
                        })
        );
        logger.info("ME_GET SUCCESS userId=" + Slf4jLoggingAdapter.shortId(userId));
        return resp;
    }

    private String generateRandomPassword() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@!#$%&";
        String password = RandomStringUtils.random(14, characters);
        String regex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@!#$%&])(?=\\S+$).{8,}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(password);
        return matcher.matches() ? password : generateRandomPassword();
    }
}