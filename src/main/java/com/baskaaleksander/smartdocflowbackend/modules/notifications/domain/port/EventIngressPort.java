package com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.port;

import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.model.event.NotificationEvent;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.model.event.PasswordResetEvent;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.model.event.UserRegisteredEvent;

public interface EventIngressPort {
    void onNotificationSent(NotificationEvent event);
    void onPasswordReset(PasswordResetEvent event);
    void onUserRegistered(UserRegisteredEvent event);
}
