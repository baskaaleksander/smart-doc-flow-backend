package com.baskaaleksander.smartdocflowbackend.modules.auth.application;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.security.CustomUserDetails;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.AuthUser;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port.AuthUserQueryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private AuthUserQueryPort userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void loadUserByUsername_success_active() {
        UUID id = UUID.randomUUID();
        AuthUser user = new AuthUser();
        user.setId(id);
        user.setUsername("john");
        user.setPassword("ENC");
        user.setRoles(new HashSet<>(Set.of("ROLE_USER", "ROLE_ADMIN")));
        user.setActive(true);
        when(userRepository.findUserByUsernameWithRoles("john")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("john");

        assertThat(details).isInstanceOf(CustomUserDetails.class);
        CustomUserDetails cud = (CustomUserDetails) details;
        assertThat(cud.getId()).isEqualTo(id);
        assertThat(cud.getUsername()).isEqualTo("john");
        assertThat(cud.getPassword()).isEqualTo("ENC");
        assertThat(cud.isEnabled()).isTrue();
        assertThat(cud.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void loadUserByUsername_success_inactive() {
        UUID id = UUID.randomUUID();
        AuthUser user = new AuthUser();
        user.setId(id);
        user.setUsername("kate");
        user.setPassword("PWD");
        user.setRoles(new HashSet<>(Set.of("ROLE_REVIEW")));
        user.setActive(false);
        when(userRepository.findUserByUsernameWithRoles("kate")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("kate");

        CustomUserDetails cud = (CustomUserDetails) details;
        assertThat(cud.isEnabled()).isFalse();
        assertThat(cud.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_REVIEW");
    }

    @Test
    void loadUserByUsername_notFound() {
        when(userRepository.findUserByUsernameWithRoles("absent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("absent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User with username absent not found");
    }
}