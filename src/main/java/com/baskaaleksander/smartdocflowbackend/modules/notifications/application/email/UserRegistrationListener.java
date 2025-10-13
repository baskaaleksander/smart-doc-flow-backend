package com.baskaaleksander.smartdocflowbackend.modules.notifications.application.email;

import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.UserRegisteredEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class UserRegistrationListener {

    private final EmailService emailService;

    @Value(value = "${app.frontend-url}")
    private String frontendUrl;

    public UserRegistrationListener(
            EmailService emailService
    ) {
        this.emailService = emailService;
    }

    @EventListener
    public void handleUserRegisteredEvent(UserRegisteredEvent event) {
        try {
            emailService.sendPlainText(event.email(), "Your password", "Your username: + " + event.username() + " your password: " + event.password() + " service url: " + frontendUrl);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
