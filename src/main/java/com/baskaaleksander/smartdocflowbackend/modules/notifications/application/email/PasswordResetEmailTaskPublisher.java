package com.baskaaleksander.smartdocflowbackend.modules.notifications.application.email;

import com.baskaaleksander.smartdocflowbackend.common.config.QueueConfig;
import com.baskaaleksander.smartdocflowbackend.modules.contracts.PasswordResetEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetEmailTaskPublisher {

    private final RabbitTemplate rabbitTemplate;

    public PasswordResetEmailTaskPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void enqueue(PasswordResetEvent event) {
        rabbitTemplate.convertAndSend(
                QueueConfig.EXCHANGE,
                QueueConfig.PASSWORD_RESET_EMAIL_ROUTING_KEY,
                event,
                message -> {
                    message.getMessageProperties().setHeader("email", event.email());
                    return message;
                }
        );
    }
}
