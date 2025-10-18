package com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.port;

import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.model.event.NotificationEvent;

public interface NotificationEventIngressPort {
    void onNotificationSent(NotificationEvent event);

}
