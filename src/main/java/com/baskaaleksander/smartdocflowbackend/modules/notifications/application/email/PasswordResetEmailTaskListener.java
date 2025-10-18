package com.baskaaleksander.smartdocflowbackend.modules.notifications.application.email;

import com.baskaaleksander.smartdocflowbackend.common.config.QueueConfig;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.model.event.PasswordResetEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetEmailTaskListener {

    private final EmailService emailService;

    @Value(value = "${app.frontend-url}")
    private String frontendUrl;

    public PasswordResetEmailTaskListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @RabbitListener(queues = QueueConfig.PASSWORD_RESET_EMAIL_QUEUE)
    public void handle(PasswordResetEvent event) {
        try {
            emailService.sendPlainText(event.email(), "Password reset link", "Your password reset link is: " + frontendUrl + "/reset-password?token=" + event.token());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
