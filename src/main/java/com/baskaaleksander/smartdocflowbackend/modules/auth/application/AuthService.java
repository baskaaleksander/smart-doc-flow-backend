package com.baskaaleksander.smartdocflowbackend.modules.auth.application;

import com.baskaaleksander.smartdocflowbackend.common.exception.WrongPasswordException;
import com.baskaaleksander.smartdocflowbackend.modules.auth.api.dto.*;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceConflictException;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.application.email.CredentialsEmailTaskPublisher;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.UserRegisteredEvent;
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
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final CookieUtil cookieUtil;
    private final UserMapper userMapper;
    private final CredentialsEmailTaskPublisher credentialsEmailTaskPublisher;


    @Autowired
    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            RoleRepository roleRepository,
            JwtUtil jwtUtil,
            PasswordEncoder passwordEncoder,
            CookieUtil cookieUtil,
            UserMapper userMapper,
            CredentialsEmailTaskPublisher credentialsEmailTaskPublisher) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.cookieUtil = cookieUtil;
        this.userMapper = userMapper;
        this.credentialsEmailTaskPublisher = credentialsEmailTaskPublisher;
    }

    public TokenResponse loginUser(UserLoginRequest user) {
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
    public UserResponse registerUser(UserRegisterRequest user) {

        Optional<User> existingUser = userRepository.findByUsername(user.getUsername());

        if(existingUser.isPresent()) {
            throw new ResourceConflictException("User with " + user.getUsername() + " username already exists");
        }

        Optional<User> existingUser2 = userRepository.findByEmail(user.getEmail());

        if(existingUser2.isPresent()) {
            throw new ResourceConflictException("User with " + user.getEmail() + " email already exists");
        }


        String password = generateRandomPassword();
        String encodedPassword = passwordEncoder.encode(password);
        Set<Role> roles = roleRepository.findAllByRoleIn(user.getRoles());

        if (roles.size() != user.getRoles().size()) {
            throw new ResourceNotFoundException("One or more roles not found");
        }

        User newUser = new User();
        newUser.setUsername(user.getUsername());
        newUser.setEmail(user.getEmail());
        newUser.setPassword(encodedPassword);
        newUser.setRoles(roles);

        User userCreated = userRepository.save(newUser);

        credentialsEmailTaskPublisher.enqueue(new UserRegisteredEvent(userCreated.getEmail(),userCreated.getUsername(), password));

        return new UserResponse(
                userCreated.getId(),
                userCreated.getUsername(),
                userCreated.getEmail(),
                userCreated.getRoles().stream().map(Role::getRole).toList(),
                userCreated.isActive(),
                userCreated.getCreatedAt()
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


    private String generateRandomPassword() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@!#$%&";
        String password = RandomStringUtils.random( 14, characters );


        String regex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@!#$%&])(?=\\S+$).{8,}$";
        Pattern pattern = Pattern.compile( regex );
        Matcher matcher = pattern.matcher( password );

        if (matcher.matches()) {
            return password;
        } else {
            return generateRandomPassword();
        }
    }

}
