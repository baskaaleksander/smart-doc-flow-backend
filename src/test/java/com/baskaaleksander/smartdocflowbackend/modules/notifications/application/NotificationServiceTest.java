package com.baskaaleksander.smartdocflowbackend.modules.notifications.application;

import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.api.dto.NotificationResponse;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.model.NotificationType;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.mapping.NotificationMapper;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.persistence.entity.NotificationEntity;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.persistence.spring.SpringDataNotificationRepository;
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

    @Mock private SpringDataNotificationRepository notificationRepository;
    @Mock private SimpMessagingTemplate simpMessagingTemplate;
    @Mock private NotificationMapper notificationMapper;

    @InjectMocks private NotificationApplicationService notificationService;

    private NotificationEntity n1;
    private NotificationEntity n2;
    private NotificationResponse r1;
    private NotificationResponse r2;
    private final String username = "user-123";

    @BeforeEach
    void setUp() {
        n1 = new NotificationEntity();
        n2 = new NotificationEntity();

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
    }

    private void stubMapper() {
        when(notificationMapper.toNotificationResponse(any(NotificationEntity.class)))
                .thenAnswer(inv -> {
                    NotificationEntity arg = inv.getArgument(0);
                    if (arg == n1) return r1;
                    if (arg == n2) return r2;
                    return new NotificationResponse(
                            UUID.randomUUID(),
                            username,
                            NotificationType.DOCUMENT_PROCESSED,
                            "example-message",
                            false,
                            Instant.now()
                    );
                });
    }

    @Test
    void sendNotification_shouldSaveToDb_and_triggerWs() {
//        notificationService.sendNotification(username, "document_uploaded", "test");

        verify(notificationRepository).save(any(NotificationEntity.class));
        verify(simpMessagingTemplate).convertAndSendToUser(
                eq(username),
                eq("/queue/notifications"),
                any(NotificationEntity.class)
        );
    }

    @Test
    void getNotifications_shouldReturnPagedResult_whenReadIsNull() {
        PaginationRequest req = new PaginationRequest(0, 2, "createdAt", Sort.Direction.DESC);
        Pageable pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<NotificationEntity> page = new PageImpl<>(List.of(n1, n2), pageable, 5);
        when(notificationRepository.findAllByUsername(username, pageable)).thenReturn(page);

        stubMapper();

        PagingResult<NotificationResponse> res = notificationService.getNotifications(username, req, null);

        assertThat(res.content()).containsExactly(r1, r2);
        assertThat(res.totalPages()).isEqualTo(3);
        assertThat(res.totalElements()).isEqualTo(5);
        assertThat(res.size()).isEqualTo(2);
        assertThat(res.page()).isEqualTo(0);
        assertThat(res.last()).isFalse();
        assertThat(res.next()).isTrue();

        verify(notificationRepository).findAllByUsername(username, pageable);
    }

    @Test
    void getNotifications_shouldReturnPagedResult_whenReadIsTrue() {
        PaginationRequest req = new PaginationRequest(1, 2, "createdAt", Sort.Direction.DESC);
        Pageable pageable = PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<NotificationEntity> page = new PageImpl<>(List.of(n1), pageable, 4);
        when(notificationRepository.findAllByUsernameAndRead(username, true, pageable)).thenReturn(page);

        stubMapper();

        PagingResult<NotificationResponse> res = notificationService.getNotifications(username, req, true);

        assertThat(res.content()).containsExactly(r1);
        assertThat(res.totalPages()).isEqualTo(2);
        assertThat(res.totalElements()).isEqualTo(4);
        assertThat(res.size()).isEqualTo(2);
        assertThat(res.page()).isEqualTo(1);
        assertThat(res.last()).isTrue();
        assertThat(res.next()).isFalse();

        verify(notificationRepository).findAllByUsernameAndRead(username, true, pageable);
    }

    @Test
    void getNotifications_shouldReturnPagedResult_whenReadIsFalse() {
        PaginationRequest req = new PaginationRequest(0, 3, "createdAt", Sort.Direction.ASC);
        Pageable pageable = PageRequest.of(0, 3, Sort.by(Sort.Direction.ASC, "createdAt"));

        Page<NotificationEntity> page = new PageImpl<>(List.of(n2), pageable, 1);
        when(notificationRepository.findAllByUsernameAndRead(username, false, pageable)).thenReturn(page);

        stubMapper();

        PagingResult<NotificationResponse> res = notificationService.getNotifications(username, req, false);

        assertThat(res.content()).containsExactly(r2);
        assertThat(res.totalPages()).isEqualTo(1);
        assertThat(res.totalElements()).isEqualTo(1);
        assertThat(res.size()).isEqualTo(3);
        assertThat(res.page()).isEqualTo(0);
        assertThat(res.last()).isTrue();
        assertThat(res.next()).isFalse();

        verify(notificationRepository).findAllByUsernameAndRead(username, false, pageable);
    }
}