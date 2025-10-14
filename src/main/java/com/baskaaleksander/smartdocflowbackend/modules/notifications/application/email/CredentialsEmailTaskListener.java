package com.baskaaleksander.smartdocflowbackend.modules.notifications.application.email;

import com.baskaaleksander.smartdocflowbackend.common.config.QueueConfig;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.UserRegisteredEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CredentialsEmailTaskListener {

    private final EmailService emailService;

    @Value(value = "${app.frontend-url}")
    private String frontendUrl;

    public CredentialsEmailTaskListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @RabbitListener(queues = QueueConfig.CREDENTIALS_EMAIL_QUEUE)
    public void handle(UserRegisteredEvent event) {
        try {
            emailService.sendPlainText(event.email(), "Your password", "Your username: " + event.username() + " your password: " + event.password() + " service url: " + frontendUrl);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
