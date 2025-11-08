package com.baskaaleksander.smartdocflowbackend.modules.auth.application;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceConflictException;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.logging.LoggingPort;
import com.baskaaleksander.smartdocflowbackend.common.security.CustomUserDetails;
import com.baskaaleksander.smartdocflowbackend.common.util.CookieUtil;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api.AuthUserApiMapper;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api.dto.*;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.AuthUser;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.Role;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.Tokens;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port.*;
import com.baskaaleksander.smartdocflowbackend.modules.contracts.UserRegisteredEvent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthApplicationServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private TokenUtilPort tokenUtil;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CookieUtil cookieUtil;
    @Mock
    private AuthEventPublisherPort authEventPublisherPort;
    @Mock
    private AuthUserCommandPort authUserCommandPort;
    @Mock
    private AuthUserQueryPort authUserQueryPort;
    @Mock
    private RoleQueryPort roleQueryPort;
    @Mock
    private AuthUserApiMapper mapper;
    @Mock
    private LoggingPort logger;

    @InjectMocks
    private AuthApplicationService service;

    private UUID userId;
    private AuthUser authUser;
    private UserResponse userResponseDto;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        authUser = new AuthUser();
        authUser.setId(userId);
        authUser.setUsername("john");
        authUser.setEmail("john@doe.com");
        authUser.setPassword("ENC");
        authUser.setRoles(new HashSet<>(Set.of("ROLE_USER")));
        authUser.setActive(true);
        userResponseDto = mock(UserResponse.class);
    }

    @Test
    void loginUser_success() {
        UserLoginRequest req = new UserLoginRequest("john", "pwd");

        CustomUserDetails principal = mock(CustomUserDetails.class);
        when(principal.getUsername()).thenReturn("john");
        when(principal.getAuthorities()).thenReturn(List.of());

        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal,
                "x",
                principal.getAuthorities()
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);

        Tokens tokens = mock(Tokens.class);
        when(tokens.getAccessToken()).thenReturn("acc");
        when(tokens.getRefreshToken()).thenReturn("ref");
        when(tokenUtil.issueTokens("john")).thenReturn(tokens);

        TokenResponse resp = service.loginUser(req);

        assertThat(resp.accessToken()).isEqualTo("acc");
        assertThat(resp.refreshToken()).isEqualTo("ref");
    }

    @Test
    void registerUser_success() {
        UserRegisterRequest req = new UserRegisterRequest("john", "john@doe.com", Set.of("ROLE_USER", "ROLE_ADMIN"));
        when(authUserQueryPort.findUserByUsernameWithRoles("john")).thenReturn(Optional.empty());
        when(authUserQueryPort.findByEmail("john@doe.com")).thenReturn(Optional.empty());
        Role r1 = new Role();
        r1.setRole("ROLE_USER");
        Role r2 = new Role();
        r2.setRole("ROLE_ADMIN");
        when(roleQueryPort.findAllByRoleIn(req.roles())).thenReturn(new HashSet<>(Set.of(r1, r2)));
        when(passwordEncoder.encode(anyString())).thenReturn("ENC");
        when(authUserCommandPort.save(any(AuthUser.class))).thenAnswer(inv -> {
            AuthUser u = inv.getArgument(0);
            u.setId(userId);
            return u;
        });
        when(mapper.toResponse(any(AuthUser.class))).thenReturn(userResponseDto);
        ArgumentCaptor<UserRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);

        UserResponse out = service.registerUser(req);

        assertThat(out).isEqualTo(userResponseDto);
        verify(authEventPublisherPort).publish(eventCaptor.capture());
        String generated = eventCaptor.getValue().password();
        assertThat(generated).hasSize(14);
        assertThat(generated).matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@!#$%&])(?=\\S+$).{8,}$");
        verify(passwordEncoder).encode(generated);
        verify(authUserCommandPort).save(argThat(u ->
                u.getUsername().equals("john")
                        && u.getEmail().equals("john@doe.com")
                        && u.getPassword().equals("ENC")
                        && u.getRoles().containsAll(Set.of("ROLE_USER", "ROLE_ADMIN"))
                        && u.isActive()
        ));
    }

    @Test
    void registerUser_usernameExists() {
        UserRegisterRequest req = new UserRegisterRequest("john", "x@y.com", Set.of("ROLE_USER"));
        when(authUserQueryPort.findUserByUsernameWithRoles("john")).thenReturn(Optional.of(authUser));

        assertThatThrownBy(() -> service.registerUser(req)).isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void registerUser_emailExists() {
        UserRegisterRequest req = new UserRegisterRequest("newuser", "john@doe.com", Set.of("ROLE_USER"));
        when(authUserQueryPort.findUserByUsernameWithRoles("newuser")).thenReturn(Optional.empty());
        when(authUserQueryPort.findByEmail("john@doe.com")).thenReturn(Optional.of(authUser));

        assertThatThrownBy(() -> service.registerUser(req)).isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void registerUser_roleMissing() {
        UserRegisterRequest req = new UserRegisterRequest("johnny", "j@d.com", Set.of("ROLE_USER", "ROLE_X"));
        when(authUserQueryPort.findUserByUsernameWithRoles("johnny")).thenReturn(Optional.empty());
        when(authUserQueryPort.findByEmail("j@d.com")).thenReturn(Optional.empty());
        Role r1 = new Role();
        r1.setRole("ROLE_USER");
        when(roleQueryPort.findAllByRoleIn(req.roles())).thenReturn(new HashSet<>(Set.of(r1)));

        assertThatThrownBy(() -> service.registerUser(req)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void refreshAccessToken_success() {
        Tokens tokens = mock(Tokens.class);
        when(tokens.getAccessToken()).thenReturn("newAcc");
        when(tokens.getRefreshToken()).thenReturn("newRef");
        when(tokenUtil.refreshAccessToken("rt")).thenReturn(tokens);

        TokenResponse resp = service.refreshAccessToken("rt");

        assertThat(resp.accessToken()).isEqualTo("newAcc");
        assertThat(resp.refreshToken()).isEqualTo("newRef");
    }

    @Test
    void logoutUser_success() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(cookieUtil.parseRefreshTokenCookie(req)).thenReturn("rtok");

        AuthActionResponse out = service.logoutUser(req, resp, UUID.randomUUID());

        assertThat(out.completed()).isTrue();
        verify(cookieUtil).clearRefreshTokenCookie(resp);
        verify(tokenUtil).invalidateRefreshToken("rtok");
    }

    @Test
    void getMe_success() {
        when(authUserQueryPort.findByIdWithRoles(userId)).thenReturn(Optional.of(authUser));
        when(mapper.toResponse(authUser)).thenReturn(userResponseDto);

        UserResponse out = service.getMe(userId);

        assertThat(out).isEqualTo(userResponseDto);
    }

    @Test
    void getMe_notFound() {
        when(authUserQueryPort.findByIdWithRoles(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMe(userId)).isInstanceOf(ResourceNotFoundException.class);
    }
}