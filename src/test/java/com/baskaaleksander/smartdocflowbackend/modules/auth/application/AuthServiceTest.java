package com.baskaaleksander.smartdocflowbackend.modules.auth.application;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceConflictException;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.security.JwtUtil;
import com.baskaaleksander.smartdocflowbackend.common.util.CookieUtil;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api.dto.TokenResponse;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api.dto.UserLoginRequest;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api.dto.UserRegisterRequest;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api.dto.UserResponse;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.entity.RoleEntity;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.spring.SpringDataRoleRepository;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.api.UserMapper;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.entity.UserEntity;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.spring.SpringDataUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private SpringDataUserRepository userRepository;
    @Mock
    private SpringDataRoleRepository roleRepository;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CookieUtil cookieUtil;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private AuthApplicationService authService;

    @Mock
    private Authentication authentication;
    @Mock
    private UserDetails userDetails;

    private UserLoginRequest loginRequest;
    private UserRegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        loginRequest = new UserLoginRequest("john", "secret");
        registerRequest = new UserRegisterRequest("newuser", "newuser@example.com", Set.of("ROLE_USER"));
    }

    @Test
    void loginUser_shouldReturnTokens_whenAuthOk() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("john");
        TokenResponse expected = new TokenResponse("access", "refresh");
        when(jwtUtil.issueTokens("john")).thenReturn(expected);

        TokenResponse res = authService.loginUser(loginRequest);

        assertThat(res).isSameAs(expected);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil).issueTokens("john");
    }

    @Test
    void loginUser_shouldThrowException_whenAuthNotOk() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.loginUser(loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Bad credentials");

        verify(jwtUtil, never()).issueTokens(anyString());
    }

    @Test
    void registerUser_shouldThrowException_whenUserExists() {
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.of(new UserEntity()));


        assertThatThrownBy(() -> authService.registerUser(registerRequest)).isInstanceOf(ResourceConflictException.class);

        verify(userRepository).findByUsername("newuser");
        verifyNoMoreInteractions(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void registerUser_shouldThrowException_whenRoleNotFound() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(roleRepository.findAllByRoleIn(anySet())).thenReturn(Set.of());

        assertThatThrownBy(() -> authService.registerUser(registerRequest)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void registerUser_shouldReturnUserResponse_whenOk() {
        RoleEntity role = new RoleEntity();
        role.setRole("ROLE_USER");

        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(roleRepository.findAllByRoleIn(Set.of("ROLE_USER"))).thenReturn(Set.of(role));
        UserEntity saved = new UserEntity();
        saved.setId(UUID.randomUUID());
        saved.setUsername(registerRequest.username());
        saved.setPassword("encoded");
        saved.setRoles(Set.of(role));
        saved.setActive(true);

        when(userRepository.save(any(UserEntity.class))).thenReturn(saved);
        UserResponse res = authService.registerUser(registerRequest);

        assertThat(res.username()).isEqualTo(registerRequest.username());
        assertThat(res.roles()).containsExactly("ROLE_USER");
        assertThat(res.active()).isTrue();
        assertThat(res.id()).isEqualTo(saved.getId());
    }

    @Test
    void refreshAccessToken_shouldReturnAccessToken() {
        TokenResponse tokenResponse = new TokenResponse("access", "refresh");;
        when(jwtUtil.refreshAccessToken("refresh")).thenReturn(tokenResponse);

        TokenResponse res = authService.refreshAccessToken("refresh");

        assertThat(res.refreshToken()).isEqualTo(tokenResponse.refreshToken());
        assertThat(res.accessToken()).isEqualTo(tokenResponse.accessToken());
    }

    @Test
    void getMe_shouldReturnMappedUserResponse() {

        UUID id = UUID.randomUUID();
        RoleEntity role = new RoleEntity();
        role.setRole("ROLE_USER");
        UserEntity entity = new UserEntity();
        entity.setId(id);
        entity.setUsername("john");
        entity.setRoles(Set.of(role));

        when(userRepository.findUserByIdWithRoles(id)).thenReturn(Optional.of(entity));

        UserResponse mapped = new UserResponse(UUID.randomUUID(), "john", "john@example.com", List.of("ROLE_USER"), true, Instant.now());
        when(userMapper.toUserResponse(entity)).thenReturn(mapped);

        UserResponse res = authService.getMe(entity.getId());

        assertThat(res.id()).isEqualTo(mapped.id());
        assertThat(res.username()).isEqualTo(mapped.username());
        assertThat(res.roles()).isEqualTo(mapped.roles());
        assertThat(res.active()).isEqualTo(mapped.active());
        verify(userRepository).findUserByIdWithRoles(id);
        verify(userMapper).toUserResponse(entity);
    }

    @Test
    void getMe_shouldThrowException() {

        UUID userId = UUID.randomUUID();

        when(userRepository.findUserByIdWithRoles(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getMe(userId)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void logoutUser_shouldEndWithSuccess() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(cookieUtil.parseRefreshTokenCookie(request)).thenReturn("refresh-token");

        String message = authService.logoutUser(request, response);

        assertThat(message).isEqualTo("Logout successful");
        verify(cookieUtil).parseRefreshTokenCookie(request);
        verify(cookieUtil).clearRefreshTokenCookie(response);
        verify(jwtUtil).invalidateRefreshToken("refresh-token");
    }

    @Test
    void logoutUser_shouldProceedWithoutRefreshToken() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(cookieUtil.parseRefreshTokenCookie(request)).thenReturn(null);

        String message = authService.logoutUser(request, response);

        assertThat(message).isEqualTo("Logout successful");
        verify(cookieUtil).parseRefreshTokenCookie(request);
        verify(cookieUtil).clearRefreshTokenCookie(response);
        verify(jwtUtil).invalidateRefreshToken(null);
    }
}
