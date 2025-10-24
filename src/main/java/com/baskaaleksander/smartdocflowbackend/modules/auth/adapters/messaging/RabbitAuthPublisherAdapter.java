package com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.messaging;

import com.baskaaleksander.smartdocflowbackend.common.config.QueueConfig;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port.AuthEventPublisherPort;
import com.baskaaleksander.smartdocflowbackend.modules.contracts.PasswordResetEvent;
import com.baskaaleksander.smartdocflowbackend.modules.contracts.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RabbitAuthPublisherAdapter implements AuthEventPublisherPort {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(PasswordResetEvent event) {
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

    @Override
    public void publish(UserRegisteredEvent event) {
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
