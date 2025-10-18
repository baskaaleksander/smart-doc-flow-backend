package com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.port;

import com.baskaaleksander.smartdocflowbackend.modules.contracts.PasswordResetEvent;
import com.baskaaleksander.smartdocflowbackend.modules.contracts.UserRegisteredEvent;

public interface AuthEventIngressPort {
    void onPasswordReset(PasswordResetEvent event);
    void onUserRegistered(UserRegisteredEvent event);
}
