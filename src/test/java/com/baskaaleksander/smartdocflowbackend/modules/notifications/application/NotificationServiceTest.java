package com.baskaaleksander.smartdocflowbackend.modules.notifications.application;

import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.NotificationType;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.mapping.NotificationMapper;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.persistence.Notification;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.persistence.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

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

    @Test
    void sendNotification_shouldSaveToDb_and_triggerWs() {
        notificationService.sendNotification("user-123", "document_uploaded", "test");

        verify(notificationRepository).save(any(Notification.class));
        verify(simpMessagingTemplate).convertAndSendToUser(eq("user-123"), eq("/queue/notifications"), any(Notification.class));

    }
}
