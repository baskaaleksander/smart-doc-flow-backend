package com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.messaging;

import com.baskaaleksander.smartdocflowbackend.common.config.QueueConfig;
import com.baskaaleksander.smartdocflowbackend.modules.contracts.PasswordResetEvent;
import com.baskaaleksander.smartdocflowbackend.modules.contracts.UserRegisteredEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitAuthPublisherAdapterTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private RabbitAuthPublisherAdapter adapter;

    @Test
    void publishPasswordResetEvent_sendsToProperExchangeAndRoutingKey() {
        PasswordResetEvent event = new PasswordResetEvent("john@doe.com", "token123");

        adapter.publish(event);

        ArgumentCaptor<String> exchangeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> routingCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> messageCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<org.springframework.amqp.core.MessagePostProcessor> processorCaptor =
                ArgumentCaptor.forClass(org.springframework.amqp.core.MessagePostProcessor.class);

        verify(rabbitTemplate).convertAndSend(
                exchangeCaptor.capture(),
                routingCaptor.capture(),
                messageCaptor.capture(),
                processorCaptor.capture()
        );

        assertThat(exchangeCaptor.getValue()).isEqualTo(QueueConfig.EXCHANGE);
        assertThat(routingCaptor.getValue()).isEqualTo(QueueConfig.PASSWORD_RESET_EMAIL_ROUTING_KEY);
        assertThat(messageCaptor.getValue()).isEqualTo(event);

        MessageProperties props = new MessageProperties();
        Message message = new Message(new byte[0], props);
        processorCaptor.getValue().postProcessMessage(message);

        assertThat(props.getHeaders().get("email")).isEqualTo("john@doe.com");
    }

    @Test
    void publishUserRegisteredEvent_sendsToProperExchangeAndRoutingKey() {
        UserRegisteredEvent event = new UserRegisteredEvent("alice@doe.com", "alice", "pass123");

        adapter.publish(event);

        ArgumentCaptor<String> exchangeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> routingCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> messageCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<org.springframework.amqp.core.MessagePostProcessor> processorCaptor =
                ArgumentCaptor.forClass(org.springframework.amqp.core.MessagePostProcessor.class);

        verify(rabbitTemplate).convertAndSend(
                exchangeCaptor.capture(),
                routingCaptor.capture(),
                messageCaptor.capture(),
                processorCaptor.capture()
        );

        assertThat(exchangeCaptor.getValue()).isEqualTo(QueueConfig.EXCHANGE);
        assertThat(routingCaptor.getValue()).isEqualTo(QueueConfig.CREDENTIALS_EMAIL_ROUTING_KEY);
        assertThat(messageCaptor.getValue()).isEqualTo(event);

        MessageProperties props = new MessageProperties();
        Message message = new Message(new byte[0], props);
        processorCaptor.getValue().postProcessMessage(message);

        assertThat(props.getHeaders().get("email")).isEqualTo("alice@doe.com");
    }
}