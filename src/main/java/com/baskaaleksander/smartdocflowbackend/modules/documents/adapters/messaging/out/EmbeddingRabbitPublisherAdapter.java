package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.messaging.out;

import com.baskaaleksander.smartdocflowbackend.common.config.QueueConfig;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.event.EmbedTask;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.EmbeddingTaskPublisherPort;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class EmbeddingRabbitPublisherAdapter implements EmbeddingTaskPublisherPort {

    private final RabbitTemplate rabbitTemplate;

    public EmbeddingRabbitPublisherAdapter(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(EmbedTask task) {
        rabbitTemplate.convertAndSend(
                QueueConfig.EXCHANGE,
                QueueConfig.EMBED_ROUTING_KEY,
                task,
                message -> {
                    message.getMessageProperties().setHeader("documentId", task.documentId().toString());
                    return message;
                }
        );
    }
}
