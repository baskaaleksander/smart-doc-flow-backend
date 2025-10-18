package com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port;

import com.baskaaleksander.smartdocflowbackend.modules.contracts.PasswordResetEvent;
import com.baskaaleksander.smartdocflowbackend.modules.contracts.UserRegisteredEvent;

public interface AuthEventPublisherPort {
    void publish(PasswordResetEvent event);
    void publish(UserRegisteredEvent event);
}
