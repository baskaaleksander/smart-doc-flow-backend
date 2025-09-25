package com.baskaaleksander.smartdocflowbackend.modules.documents.application.impl;

import com.baskaaleksander.smartdocflowbackend.common.config.QueueConfig;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.OcrTask;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
public class OcrTaskListener {
    private final OcrService ocrService;

    public OcrTaskListener(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    @RabbitListener(queues = QueueConfig.OCR_QUEUE)
    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public void handle(OcrTask task) {
        ocrService.runOcr(task.documentId());
    }
}
