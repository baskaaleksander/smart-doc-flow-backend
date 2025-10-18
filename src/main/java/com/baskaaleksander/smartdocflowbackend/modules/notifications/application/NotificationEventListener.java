package com.baskaaleksander.smartdocflowbackend.modules.notifications.application;

import com.baskaaleksander.smartdocflowbackend.modules.contracts.NotificationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener {

    private final NotificationApplicationService notificationService;

    public NotificationEventListener(
            NotificationApplicationService notificationService
    ) {
        this.notificationService = notificationService;
    }

    @EventListener
    public void handleNotificationEvent(NotificationEvent event) {
        notificationService.sendNotification(event.username(), event.type(), event.message());
    }
}
