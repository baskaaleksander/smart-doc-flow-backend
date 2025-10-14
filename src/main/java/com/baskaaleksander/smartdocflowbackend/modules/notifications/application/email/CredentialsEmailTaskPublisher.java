package com.baskaaleksander.smartdocflowbackend.modules.notifications.application.email;

import com.baskaaleksander.smartdocflowbackend.common.config.QueueConfig;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.UserRegisteredEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class CredentialsEmailTaskPublisher {

    private final RabbitTemplate rabbitTemplate;

    public CredentialsEmailTaskPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void enqueue(UserRegisteredEvent event) {
        rabbitTemplate.convertAndSend(
                QueueConfig.EXCHANGE,
                QueueConfig.CREDENTIALS_EMAIL_ROUTING_KEY,
                event,
                message -> {
                    message.getMessageProperties().setHeader("email", event.email());
                    return message;
                }
        );
    }
}
