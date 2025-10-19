package com.baskaaleksander.smartdocflowbackend.modules.documents.application.ocr;

import com.baskaaleksander.smartdocflowbackend.common.config.QueueConfig;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.event.OcrTask;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OcrTaskPublisher {
    private final RabbitTemplate rabbitTemplate;

    public OcrTaskPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void enqueue(UUID documentId) {
        OcrTask task = new OcrTask(documentId);
        rabbitTemplate.convertAndSend(
                QueueConfig.EXCHANGE,
                QueueConfig.OCR_ROUTING_KEY,
                task,
                message -> {
                    message.getMessageProperties().setHeader("documentId", documentId.toString());
                    return message;
                }
        );
    }
}
