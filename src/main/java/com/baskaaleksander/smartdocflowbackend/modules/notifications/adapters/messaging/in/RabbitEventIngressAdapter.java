package com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.messaging.in;

import com.baskaaleksander.smartdocflowbackend.common.config.QueueConfig;
import com.baskaaleksander.smartdocflowbackend.modules.contracts.PasswordResetEvent;
import com.baskaaleksander.smartdocflowbackend.modules.contracts.UserRegisteredEvent;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.port.AuthEventIngressPort;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RabbitEventIngressAdapter {

    private final AuthEventIngressPort ingress;

    public RabbitEventIngressAdapter(AuthEventIngressPort ingress) {
        this.ingress = ingress;
    }

    @RabbitListener(queues = QueueConfig.CREDENTIALS_EMAIL_QUEUE)
    public void handleRegistered(UserRegisteredEvent event) {
        ingress.onUserRegistered(event);
    }

    @RabbitListener(queues = QueueConfig.PASSWORD_RESET_EMAIL_QUEUE)
    public void handleReset(PasswordResetEvent event) {
        ingress.onPasswordReset(event);
    }
}
