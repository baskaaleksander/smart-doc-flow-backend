package com.baskaaleksander.smartdocflowbackend.modules.documents.application.embed;

import com.baskaaleksander.smartdocflowbackend.common.config.QueueConfig;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.event.EmbedTask;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EmbedTaskPublisher {
    private final RabbitTemplate rabbitTemplate;

    public EmbedTaskPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void enqueue(UUID documentId) {
        EmbedTask task = new EmbedTask(documentId);

        rabbitTemplate.convertAndSend(
                QueueConfig.EXCHANGE,
                QueueConfig.EMBED_ROUTING_KEY,
                task,
                message -> {
                    message.getMessageProperties().setHeader("documentId", documentId.toString());
                    return message;
                }
        );
    }
}
