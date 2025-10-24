package com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.utils;

import com.baskaaleksander.smartdocflowbackend.common.exception.InvalidJwtTokenException;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.entity.RefreshTokenEntity;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.spring.SpringDataRefreshTokenRepository;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.Tokens;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.entity.UserEntity;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.spring.SpringDataUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenUtilAdapterTest {

    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private SpringDataRefreshTokenRepository refreshTokenRepository;
    @Mock
    private SpringDataUserRepository userRepository;

    private JwtTokenUtilAdapter adapter;

    private final String accessSecret = "A".repeat(64);
    private final String refreshSecret = "R".repeat(64);

    @BeforeEach
    void setup() throws Exception {
        adapter = new JwtTokenUtilAdapter(userDetailsService, refreshTokenRepository, userRepository);
        setField(adapter, "jwtAccessSecret", accessSecret);
        setField(adapter, "jwtRefreshSecret", refreshSecret);
        setField(adapter, "jwtAccessExpiration", 60_000);   // 60s
        setField(adapter, "jwtRefreshExpiration", 600_000); // 10min
        adapter.init();
    }

    @Test
    void issueTokens_success() {
        String username = "john";
        when(userDetailsService.loadUserByUsername(username))
                .thenReturn(User.withUsername(username).password("x").authorities("ROLE_USER").build());
        UserEntity ue = new UserEntity();
        ue.setId(UUID.randomUUID());
        ue.setUsername(username);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(ue));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Tokens tokens = adapter.issueTokens(username);

        assertThat(tokens.getAccessToken()).isNotBlank();
        assertThat(tokens.getRefreshToken()).isNotBlank();
        assertThat(adapter.validateAccessToken(tokens.getAccessToken())).isTrue();
        assertThat(adapter.getUsernameFromAccessToken(tokens.getAccessToken())).isEqualTo(username);
    }

    @Test
    void generateAccessToken_containsSubjectAndIsValid() {
        String username = "alice";
        when(userDetailsService.loadUserByUsername(username))
                .thenReturn(User.withUsername(username).password("y").authorities("ROLE_USER", "ROLE_ADMIN").build());

        String at = adapter.generateAccessToken(username);

        assertThat(adapter.validateAccessToken(at)).isTrue();
        assertThat(adapter.getUsernameFromAccessToken(at)).isEqualTo(username);
    }

    @Test
    void refreshAccessToken_success() {
        String username = "mark";
        when(userDetailsService.loadUserByUsername(username))
                .thenReturn(User.withUsername(username).password("z").authorities("ROLE_USER").build());
        UserEntity ue = new UserEntity();
        ue.setId(UUID.randomUUID());
        ue.setUsername(username);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(ue));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Tokens issued = adapter.issueTokens(username);
        String oldJti = adapter.getJtiFromRefreshToken(issued.getRefreshToken());

        RefreshTokenEntity rt = new RefreshTokenEntity(
                oldJti,
                ue,
                LocalDateTime.now().plusMinutes(5),
                false,
                null,
                LocalDateTime.now()
        );
        when(refreshTokenRepository.findByJtiAndRevokedFalse(oldJti)).thenReturn(Optional.of(rt));

        Tokens refreshed = adapter.refreshAccessToken(issued.getRefreshToken());

        assertThat(refreshed.getAccessToken()).isNotBlank();
        assertThat(refreshed.getRefreshToken()).isNotBlank();
        assertThat(refreshed.getRefreshToken()).isNotEqualTo(issued.getRefreshToken());
        assertThat(rt.isRevoked()).isTrue();
        verify(refreshTokenRepository, atLeastOnce()).save(any());
    }

    @Test
    void validateRefreshToken_expiredInDb() {
        String username = "bob";

        String token = adapter.generateRefreshToken(username, UUID.randomUUID().toString());
        String jti = adapter.getJtiFromRefreshToken(token);

        UserEntity ue = new UserEntity();
        ue.setId(UUID.randomUUID());
        ue.setUsername(username);

        RefreshTokenEntity expired = new RefreshTokenEntity(
                jti,
                ue,
                LocalDateTime.now().minusMinutes(1),
                false,
                null,
                LocalDateTime.now().minusMinutes(2)
        );
        when(refreshTokenRepository.findByJtiAndRevokedFalse(jti)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> adapter.validateRefreshToken(token))
                .isInstanceOf(InvalidJwtTokenException.class)
                .hasMessageContaining("expired (DB)");
    }

    @Test
    void validateRefreshToken_expiredJwt() throws Exception {
        adapter = new JwtTokenUtilAdapter(userDetailsService, refreshTokenRepository, userRepository);
        setField(adapter, "jwtAccessSecret", accessSecret);
        setField(adapter, "jwtRefreshSecret", refreshSecret);
        setField(adapter, "jwtAccessExpiration", 60_000);
        setField(adapter, "jwtRefreshExpiration", -1_000); // already expired
        adapter.init();

        String token = adapter.generateRefreshToken("any", UUID.randomUUID().toString());

        assertThatThrownBy(() -> adapter.validateRefreshToken(token))
                .isInstanceOf(InvalidJwtTokenException.class)
                .hasMessageContaining("expired (JWT)");
    }

    @Test
    void invalidateRefreshToken_marksRevokedAndSaves() {
        String username = "sam";
        String token = adapter.generateRefreshToken(username, UUID.randomUUID().toString());
        String jti = adapter.getJtiFromRefreshToken(token);
        RefreshTokenEntity entity = new RefreshTokenEntity(jti, null, LocalDateTime.now().plusMinutes(5), false, null, LocalDateTime.now());
        when(refreshTokenRepository.findByJtiAndRevokedFalse(jti)).thenReturn(Optional.of(entity));

        adapter.invalidateRefreshToken(token);

        assertThat(entity.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(entity);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}