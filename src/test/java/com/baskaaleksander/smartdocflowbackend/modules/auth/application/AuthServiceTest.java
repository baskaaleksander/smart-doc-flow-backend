package com.baskaaleksander.smartdocflowbackend.modules.auth.application;

import com.baskaaleksander.smartdocflowbackend.common.security.JwtUtil;
import com.baskaaleksander.smartdocflowbackend.common.util.CookieUtil;
import com.baskaaleksander.smartdocflowbackend.modules.auth.api.dto.UserRequest;
import com.baskaaleksander.smartdocflowbackend.modules.auth.persistence.RoleRepository;
import com.baskaaleksander.smartdocflowbackend.modules.users.mapping.UserMapper;
import com.baskaaleksander.smartdocflowbackend.modules.users.persistence.UserRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

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
}
