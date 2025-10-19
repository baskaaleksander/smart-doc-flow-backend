package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.messaging.out;

import com.baskaaleksander.smartdocflowbackend.common.config.QueueConfig;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.event.OcrTask;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.OcrTaskPublisherPort;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class OcrRabbitPublisherAdapter implements OcrTaskPublisherPort {

    private final RabbitTemplate rabbitTemplate;

    public OcrRabbitPublisherAdapter(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void handle(OcrTask task) {
        rabbitTemplate.convertAndSend(
                QueueConfig.EXCHANGE,
                QueueConfig.OCR_ROUTING_KEY,
                task,
                message -> {
                    message.getMessageProperties().setHeader("documentId", task.documentId().toString());
                    return message;
                }
        );
    }
}
