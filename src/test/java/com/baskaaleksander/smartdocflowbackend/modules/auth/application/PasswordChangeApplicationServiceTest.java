package com.baskaaleksander.smartdocflowbackend.modules.auth.application;

import com.baskaaleksander.smartdocflowbackend.common.exception.InvalidResetTokenException;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.exception.WrongPasswordException;
import com.baskaaleksander.smartdocflowbackend.common.logging.LoggingPort;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api.dto.AuthActionResponse;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api.dto.IsTokenValidResponse;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api.dto.ResetPasswordRequest;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api.dto.UpdatePasswordRequest;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.AuthUser;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.PasswordResetToken;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port.*;
import com.baskaaleksander.smartdocflowbackend.modules.contracts.PasswordResetEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordChangeApplicationServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthEventPublisherPort authEventPublisherPort;
    @Mock
    private Clock clock;
    @Mock
    private AuthUserQueryPort authUserQueryPort;
    @Mock
    private AuthUserCommandPort authUserCommandPort;
    @Mock
    private PasswordResetTokenCommandPort passwordResetTokenCommandPort;
    @Mock
    private PasswordResetTokenQueryPort passwordResetTokenQueryPort;
    @Mock
    private LoggingPort logger;

    @InjectMocks
    private PasswordChangeApplicationService service;

    private final Instant now = Instant.parse("2025-10-24T10:00:00Z");

    @BeforeEach
    void setupClock() {
        lenient().when(clock.instant()).thenReturn(now);
        lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Nested
    class UpdatePasswordByOldPassword {

        @Test
        void success() {
            UUID id = UUID.randomUUID();
            AuthUser user = new AuthUser();
            user.setId(id);
            user.setPassword("ENC_OLD");
            when(authUserQueryPort.findById(id)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("old", "ENC_OLD")).thenReturn(true);
            when(passwordEncoder.encode("new")).thenReturn("ENC_NEW");

            AuthActionResponse res = service.updatePassword(id, new UpdatePasswordRequest("old", "new"));

            assertThat(res.completed()).isTrue();
            verify(authUserCommandPort).save(argThat(u -> "ENC_NEW".equals(u.getPassword())));
        }

        @Test
        void userNotFound() {
            UUID id = UUID.randomUUID();
            when(authUserQueryPort.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updatePassword(id, new UpdatePasswordRequest("old", "new")))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void wrongOldPassword() {
            UUID id = UUID.randomUUID();
            AuthUser user = new AuthUser();
            user.setId(id);
            user.setPassword("ENC_OLD");
            when(authUserQueryPort.findById(id)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("old", "ENC_OLD")).thenReturn(false);

            assertThatThrownBy(() -> service.updatePassword(id, new UpdatePasswordRequest("old", "new")))
                    .isInstanceOf(WrongPasswordException.class);
            verify(authUserCommandPort, never()).save(any());
        }
    }

    @Nested
    class ResetPasswordByToken {

        @Test
        void success() {
            UUID uid = UUID.randomUUID();
            PasswordResetToken token = new PasswordResetToken();
            token.setUserId(uid);
            token.setToken("tok123");
            token.setRevoked(false);
            token.setExpiresAt(now.plus(Duration.ofHours(1)));
            AuthUser user = new AuthUser();
            user.setId(uid);
            when(passwordResetTokenQueryPort.findByToken("tok123")).thenReturn(Optional.of(token));
            when(authUserQueryPort.findById(uid)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("N3w!Pass")).thenReturn("ENC_NEW");

            AuthActionResponse res = service.resetPassword(new ResetPasswordRequest("tok123", "N3w!Pass"));

            assertThat(res.completed()).isTrue();
            verify(authUserCommandPort).save(argThat(u -> "ENC_NEW".equals(u.getPassword())));
            verify(passwordResetTokenCommandPort).save(argThat(t -> t.isRevoked()));
        }

        @Test
        void tokenNotFound() {
            when(passwordResetTokenQueryPort.findByToken("x")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest("x", "pass")))
                    .isInstanceOf(InvalidResetTokenException.class);
        }

        @Test
        void tokenRevoked() {
            PasswordResetToken token = new PasswordResetToken();
            token.setRevoked(true);
            token.setExpiresAt(now.plusSeconds(10));
            when(passwordResetTokenQueryPort.findByToken("x")).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest("x", "pass")))
                    .isInstanceOf(InvalidResetTokenException.class);
        }

        @Test
        void tokenExpired() {
            PasswordResetToken token = new PasswordResetToken();
            token.setRevoked(false);
            token.setExpiresAt(now.minusSeconds(1));
            when(passwordResetTokenQueryPort.findByToken("x")).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest("x", "pass")))
                    .isInstanceOf(InvalidResetTokenException.class);
        }

        @Test
        void userNotFound() {
            UUID uid = UUID.randomUUID();
            PasswordResetToken token = new PasswordResetToken();
            token.setUserId(uid);
            token.setRevoked(false);
            token.setExpiresAt(now.plusSeconds(60));
            when(passwordResetTokenQueryPort.findByToken("x")).thenReturn(Optional.of(token));
            when(authUserQueryPort.findById(uid)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest("x", "pass")))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class CheckTokenValidity {

        @Test
        void success() {
            PasswordResetToken token = new PasswordResetToken();
            token.setRevoked(false);
            token.setExpiresAt(now.plusSeconds(60));
            when(passwordResetTokenQueryPort.findByToken("ok")).thenReturn(Optional.of(token));

            IsTokenValidResponse res = service.checkToken("ok");

            assertThat(res.isValid()).isTrue();
        }

        @Test
        void tokenNotFound() {
            when(passwordResetTokenQueryPort.findByToken("x")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.checkToken("x"))
                    .isInstanceOf(InvalidResetTokenException.class);
        }

        @Test
        void tokenRevoked() {
            PasswordResetToken token = new PasswordResetToken();
            token.setRevoked(true);
            token.setExpiresAt(now.plusSeconds(60));
            when(passwordResetTokenQueryPort.findByToken("x")).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> service.checkToken("x"))
                    .isInstanceOf(InvalidResetTokenException.class);
        }

        @Test
        void tokenExpired() {
            PasswordResetToken token = new PasswordResetToken();
            token.setRevoked(false);
            token.setExpiresAt(now.minusSeconds(1));
            when(passwordResetTokenQueryPort.findByToken("x")).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> service.checkToken("x"))
                    .isInstanceOf(InvalidResetTokenException.class);
        }
    }

    @Nested
    class RequestPasswordResetFlow {

        @BeforeEach
        void initSync() {
            TransactionSynchronizationManager.initSynchronization();
        }

        @AfterEach
        void clearSync() {
            TransactionSynchronizationManager.clearSynchronization();
        }

        @Test
        void userExists_publishesEventAfterCommit() {
            String email = "john@doe.com";
            AuthUser user = new AuthUser();
            user.setId(UUID.randomUUID());
            when(authUserQueryPort.findByEmail(email)).thenReturn(Optional.of(user));
            ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
            when(passwordResetTokenCommandPort.save(any(PasswordResetToken.class))).thenAnswer(inv -> inv.getArgument(0));

            AuthActionResponse res = service.requestPasswordReset(email);

            assertThat(res.completed()).isTrue();
            verify(passwordResetTokenCommandPort).invalidateAllTokens(user.getId());
            verify(passwordResetTokenCommandPort).save(tokenCaptor.capture());
            PasswordResetToken saved = tokenCaptor.getValue();

            for (TransactionSynchronization s : TransactionSynchronizationManager.getSynchronizations()) {
                s.afterCommit();
            }

            ArgumentCaptor<PasswordResetEvent> eventCaptor = ArgumentCaptor.forClass(PasswordResetEvent.class);
            verify(authEventPublisherPort).publish(eventCaptor.capture());
            PasswordResetEvent event = eventCaptor.getValue();
            assertThat(event.email()).isEqualTo(email);
            assertThat(event.token()).isEqualTo(saved.getToken());
        }

        @Test
        void userNotFound_doesNothing() {
            String email = "absent@ex.com";
            when(authUserQueryPort.findByEmail(email)).thenReturn(Optional.empty());

            AuthActionResponse res = service.requestPasswordReset(email);

            assertThat(res.completed()).isTrue();
            verify(passwordResetTokenCommandPort, never()).invalidateAllTokens(any());
            verify(passwordResetTokenCommandPort, never()).save(any());

            for (TransactionSynchronization s : TransactionSynchronizationManager.getSynchronizations()) {
                s.afterCommit();
            }

            verify(authEventPublisherPort, never()).publish(any(PasswordResetEvent.class));
        }
    }
}