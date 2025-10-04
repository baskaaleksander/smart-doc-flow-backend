package com.baskaaleksander.smartdocflowbackend.modules.notifications.application;

import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.api.dto.NotificationResponse;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.NotificationType;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.mapping.NotificationMapper;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.persistence.Notification;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.persistence.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;
    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationService notificationService;

    private Notification n1;
    private Notification n2;
    private NotificationResponse r1;
    private NotificationResponse r2;
    private final String username = "user-123";


    @BeforeEach
    void setUp() {
        n1 = new Notification();
        n2 = new Notification();
        r1 = new NotificationResponse(
                UUID.randomUUID(),
                username,
                NotificationType.DOCUMENT_PROCESSED,
                "example-message",
                false,
                Instant.now()
        );
        r2 = new NotificationResponse(
                UUID.randomUUID(),
                username,
                NotificationType.DOCUMENT_PROCESSED,
                "example-message",
                true,
                Instant.now()
        );

        when(notificationMapper.toNotificationResponse(n1)).thenReturn(r1);
        when(notificationMapper.toNotificationResponse(n2)).thenReturn(r2);
    }

    @Test
    void sendNotification_shouldSaveToDb_and_triggerWs() {
        notificationService.sendNotification("user-123", "document_uploaded", "test");

        verify(notificationRepository).save(any(Notification.class));
        verify(simpMessagingTemplate).convertAndSendToUser(eq("user-123"), eq("/queue/notifications"), any(Notification.class));

    }

    @Test
    void getNotifications_shouldReturnPagedResult_whenReadIsNull() {
        PaginationRequest req = new PaginationRequest(0, 2, "createdAt", Sort.Direction.DESC);
        Pageable pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Notification> page = new PageImpl<>(
                List.of(n1, n2), pageable, 5
        );

        when(notificationRepository.findAllByUsername(pageable, username)).thenReturn(page);

        PagingResult<NotificationResponse> res = notificationService.getNotifications(username, req, null);

        assertThat(res.content()).containsExactly(r1, r2);
        assertThat(res.totalPages()).isEqualTo(3);
        assertThat(res.totalElements()).isEqualTo(5);
        assertThat(res.size()).isEqualTo(2);
        assertThat(res.page()).isEqualTo(0);
        assertThat(res.last()).isFalse();
        assertThat(res.next()).isTrue();
        verify(notificationRepository).findAllByUsername(pageable, username);
    }
}
