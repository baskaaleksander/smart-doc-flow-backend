package com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.port;

import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.model.Notification;

import java.util.UUID;

public interface NotificationCommandPort {
    Notification save(Notification notification);
    Integer markOneRead(String username, UUID id);
    Integer markAllRead(String username);
}
