package com.baskaaleksander.smartdocflowbackend.modules.auth.application;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceConflictException;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.security.JwtUtil;
import com.baskaaleksander.smartdocflowbackend.common.util.CookieUtil;
import com.baskaaleksander.smartdocflowbackend.modules.auth.api.dto.TokenResponse;
import com.baskaaleksander.smartdocflowbackend.modules.auth.api.dto.UserRequest;
import com.baskaaleksander.smartdocflowbackend.modules.auth.persistence.RoleRepository;
import com.baskaaleksander.smartdocflowbackend.modules.users.mapping.UserMapper;
import com.baskaaleksander.smartdocflowbackend.modules.users.persistence.User;
import com.baskaaleksander.smartdocflowbackend.modules.users.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
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
}
