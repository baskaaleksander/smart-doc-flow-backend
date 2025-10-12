package com.baskaaleksander.smartdocflowbackend.modules.notifications.application.email;

import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.UserRegisteredEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class UserRegistrationListener {

    private final EmailService emailService;

    public UserRegistrationListener(
            EmailService emailService
    ) {
        this.emailService = emailService;
    }

    @EventListener
    public void handleUserRegisteredEvent(UserRegisteredEvent event) {
        try {
            emailService.sendPlainText(event.email(), "Your password", "Your new password to service is: " + event.password());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
