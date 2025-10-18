package com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.port;

import com.baskaaleksander.smartdocflowbackend.modules.contracts.NotificationEvent;

public interface NotificationEventIngressPort {
    void onNotificationSent(NotificationEvent event);

}
