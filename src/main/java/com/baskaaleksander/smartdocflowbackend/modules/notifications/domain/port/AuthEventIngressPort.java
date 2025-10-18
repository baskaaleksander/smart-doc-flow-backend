package com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.port;

import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.model.event.PasswordResetEvent;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.model.event.UserRegisteredEvent;

public interface AuthEventIngressPort {
    void onPasswordReset(PasswordResetEvent event);
    void onUserRegistered(UserRegisteredEvent event);
}
