package com.baskaaleksander.smartdocflowbackend.modules.auth.application;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceConflictException;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.security.JwtUtil;
import com.baskaaleksander.smartdocflowbackend.common.util.CookieUtil;
import com.baskaaleksander.smartdocflowbackend.modules.auth.api.dto.TokenResponse;
import com.baskaaleksander.smartdocflowbackend.modules.auth.api.dto.UserRequest;
import com.baskaaleksander.smartdocflowbackend.modules.auth.api.dto.UserResponse;
import com.baskaaleksander.smartdocflowbackend.modules.auth.persistence.Role;
import com.baskaaleksander.smartdocflowbackend.modules.auth.persistence.RoleRepository;
import com.baskaaleksander.smartdocflowbackend.modules.users.mapping.UserMapper;
import com.baskaaleksander.smartdocflowbackend.modules.users.persistence.User;
import com.baskaaleksander.smartdocflowbackend.modules.users.persistence.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

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
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CookieUtil cookieUtil;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthService authService;

    @Mock
    private Authentication authentication;
    @Mock
    private UserDetails userDetails;

    private UserRequest loginRequest;
    private UserRequest registerRequest;

    @BeforeEach
    void setUp() {
        loginRequest = new UserRequest("john", "secret");
        registerRequest = new UserRequest("newuser", "pwd123");
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
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.of(new User()));


        assertThatThrownBy(() -> authService.registerUser(registerRequest)).isInstanceOf(ResourceConflictException.class);

        verify(userRepository).findByUsername("newuser");
        verifyNoMoreInteractions(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void registerUser_shouldThrowException_whenRoleNotFound() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encoded");
        when(roleRepository.findRoleByRole(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.registerUser(registerRequest)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void registerUser_shouldReturnUserResponse_whenOk() {
        Role role = new Role();
        role.setRole("ROLE_USER");

        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encoded");
        when(roleRepository.findRoleByRole("ROLE_USER")).thenReturn(Optional.of(role));

        User saved = new User();
        saved.setId(UUID.randomUUID());
        saved.setUsername(registerRequest.getUsername());
        saved.setPassword("encoded");
        saved.setRoles(Set.of(role));
        saved.setActive(true);

        when(userRepository.save(any(User.class))).thenReturn(saved);
        UserResponse res = authService.registerUser(registerRequest);

        assertThat(res.username()).isEqualTo(registerRequest.getUsername());
        assertThat(res.roles()).containsExactly("ROLE_USER");
        assertThat(res.isActive()).isTrue();
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
        org.springframework.security.core.userdetails.User springUser =
                new org.springframework.security.core.userdetails.User("john", "x", List.of());

        Role role = new Role();
        role.setRole("ROLE_USER");
        User entity = new User();
        entity.setUsername("john");
        entity.setRoles(Set.of(role));

        when(userRepository.findUserByUsernameWithRoles("john")).thenReturn(Optional.of(entity));

        UserResponse mapped = new UserResponse(UUID.randomUUID(), "john", List.of("ROLE_USER"), true);
        when(userMapper.toUserResponse(entity)).thenReturn(mapped);

        UserResponse res = authService.getMe(springUser);

        assertThat(res.id()).isEqualTo(mapped.id());
        assertThat(res.username()).isEqualTo(mapped.username());
        assertThat(res.roles()).isEqualTo(mapped.roles());
        assertThat(res.isActive()).isEqualTo(mapped.isActive());
        verify(userRepository).findUserByUsernameWithRoles("john");
        verify(userMapper).toUserResponse(entity);
    }

    @Test
    void getMe_shouldThrowException() {
        org.springframework.security.core.userdetails.User springUser =
                new org.springframework.security.core.userdetails.User("john", "x", List.of());

        when(userRepository.findUserByUsernameWithRoles(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getMe(springUser)).isInstanceOf(ResourceNotFoundException.class);
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
