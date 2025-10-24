package com.baskaaleksander.smartdocflowbackend.modules.notifications.application;

import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.contracts.NotificationEvent;
import com.baskaaleksander.smartdocflowbackend.modules.contracts.PasswordResetEvent;
import com.baskaaleksander.smartdocflowbackend.modules.contracts.UserRegisteredEvent;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.api.NotificationApiMapper;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.api.dto.NotificationResponse;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.api.dto.ReadNotificationRequest;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.model.Notification;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.model.NotificationType;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.port.EmailSenderPort;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.port.NotificationCommandPort;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.port.NotificationQueryPort;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.port.RealtimePushPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationApplicationServiceTest {

    @Mock
    private RealtimePushPort push;
    @Mock
    private EmailSenderPort emailPort;
    @Mock
    private NotificationCommandPort notificationCommandPort;
    @Mock
    private NotificationQueryPort notificationQueryPort;
    @Mock
    private NotificationApiMapper mapper;

    @InjectMocks
    private NotificationApplicationService service;

    @BeforeEach
    void setFrontendUrl() throws Exception {
        Field f = NotificationApplicationService.class.getDeclaredField("frontendUrl");
        f.setAccessible(true);
        f.set(service, "https://app.example.com");
    }

    @Test
    void getNotifications_all() {
        String username = "john";
        PaginationRequest req = new PaginationRequest(0, 2, null, null);
        Notification n1 = new Notification();
        Notification n2 = new Notification();
        PagingResult<Notification> page = new PagingResult<>(List.of(n1, n2), 3, 6L, 2, 0, false, true);
        NotificationResponse d1 = mock(NotificationResponse.class);
        NotificationResponse d2 = mock(NotificationResponse.class);

        when(notificationQueryPort.findAllByUsername(req, username)).thenReturn(page);
        when(mapper.toResponse(n1)).thenReturn(d1);
        when(mapper.toResponse(n2)).thenReturn(d2);

        PagingResult<NotificationResponse> out = service.getNotifications(username, req, null);

        assertThat(out.content()).containsExactly(d1, d2);
        assertThat(out.totalPages()).isEqualTo(3);
        assertThat(out.totalElements()).isEqualTo(6L);
        verify(notificationQueryPort).findAllByUsername(req, username);
        verify(notificationQueryPort, never()).findAllByUsernameAndRead(any(), anyString(), anyBoolean());
    }

    @Test
    void getNotifications_byReadFlag() {
        String username = "kate";
        PaginationRequest req = new PaginationRequest(1, 3, "createdAt,desc", null);
        Notification n1 = new Notification();
        PagingResult<Notification> page = new PagingResult<>(List.of(n1), 1, 1L, 3, 1, true, false);
        NotificationResponse d1 = mock(NotificationResponse.class);

        when(notificationQueryPort.findAllByUsernameAndRead(req, username, true)).thenReturn(page);
        when(mapper.toResponse(n1)).thenReturn(d1);

        PagingResult<NotificationResponse> out = service.getNotifications(username, req, true);

        assertThat(out.content()).containsExactly(d1);
        assertThat(out.last()).isTrue();
        verify(notificationQueryPort).findAllByUsernameAndRead(req, username, true);
    }

    @Test
    void getUnreadNotificationsCount_returnsValue() {
        when(notificationQueryPort.getNotificationsCountByUsernameAndRead("john", false)).thenReturn(7);

        Integer out = service.getUnreadNotificationsCount("john");

        assertThat(out).isEqualTo(7);
    }

    @Test
    void markAllAsRead_trueMarksAll() {
        when(notificationCommandPort.markAllRead("john")).thenReturn(5);

        Integer out = service.markAllAsRead("john", new ReadNotificationRequest(true));

        assertThat(out).isEqualTo(5);
        verify(notificationCommandPort).markAllRead("john");
    }

    @Test
    void markAllAsRead_falseNoop() {
        Integer out = service.markAllAsRead("john", new ReadNotificationRequest(false));

        assertThat(out).isEqualTo(0);
        verify(notificationCommandPort, never()).markAllRead(anyString());
    }

    @Test
    void markOneAsRead_trueMarksOne() {
        UUID id = UUID.randomUUID();
        when(notificationCommandPort.markOneRead("john", id)).thenReturn(1);

        Integer out = service.markOneAsRead("john", id, new ReadNotificationRequest(true));

        assertThat(out).isEqualTo(1);
        verify(notificationCommandPort).markOneRead("john", id);
    }

    @Test
    void markOneAsRead_falseNoop() {
        UUID id = UUID.randomUUID();

        Integer out = service.markOneAsRead("john", id, new ReadNotificationRequest(false));

        assertThat(out).isEqualTo(0);
        verify(notificationCommandPort, never()).markOneRead(anyString(), any());
    }

    @Test
    void onNotificationSent_persistsAndPushes() {
        NotificationEvent event = new NotificationEvent("john", "review_comment", "msg");
        when(notificationCommandPort.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        service.onNotificationSent(event);

        verify(notificationCommandPort).save(argThat(n ->
                "john".equals(n.getUsername()) &&
                        n.getType() == NotificationType.REVIEW_COMMENT &&
                        "msg".equals(n.getMessage())));
        verify(push).sendToUser("john", "/queue/notifications", event);
    }

    @Test
    void onPasswordReset_sendsEmail() {
        PasswordResetEvent event = new PasswordResetEvent("john@doe.com", "tok123");

        service.onPasswordReset(event);

        verify(emailPort).sendEmail(
                eq("john@doe.com"),
                eq("Password reset link"),
                eq("Your password reset link is: https://app.example.com/reset-password?token=tok123")
        );
    }

    @Test
    void onUserRegistered_sendsEmail() {
        UserRegisteredEvent event = new UserRegisteredEvent("alice@doe.com", "alice", "pwd!");

        service.onUserRegistered(event);

        verify(emailPort).sendEmail(
                eq("alice@doe.com"),
                eq("Your password"),
                eq("Your username: alice your password: pwd! service url: https://app.example.com")
        );
    }
}