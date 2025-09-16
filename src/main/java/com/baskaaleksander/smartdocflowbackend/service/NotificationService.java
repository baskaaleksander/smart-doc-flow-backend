package com.baskaaleksander.smartdocflowbackend.service;

import com.baskaaleksander.smartdocflowbackend.dto.response.NotificationResponse;
import com.baskaaleksander.smartdocflowbackend.enums.NotificationType;
import com.baskaaleksander.smartdocflowbackend.model.Notification;
import com.baskaaleksander.smartdocflowbackend.repository.NotificationRepository;
import jakarta.transaction.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public NotificationService(
            NotificationRepository notificationRepository,
            SimpMessagingTemplate simpMessagingTemplate
            ) {
        this.notificationRepository = notificationRepository;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @Transactional
    public void sendNotification(String username, String type, String message) {
        Notification notification = new Notification();

        notification.setUsername(username);
        notification.setType(NotificationType.fromString(type));
        notification.setMessage(message);

        notificationRepository.save(notification);

        simpMessagingTemplate.convertAndSendToUser(
                username,
        "/queue/notifications",
                notification
                );

    }

    public List<NotificationResponse> getAllUnreadNotifications() {
        return null;
    }
}
