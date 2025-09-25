package com.baskaaleksander.smartdocflowbackend.modules.documents.application.impl;

import com.baskaaleksander.smartdocflowbackend.common.config.QueueConfig;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.OcrTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
public class OcrTaskListener {
    private final OcrService ocrService;
    private final Logger log = LoggerFactory.getLogger(OcrTaskListener.class);

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

    @Recover
    public void recover(Exception ex, OcrTask failed) {
        log.error("OCR task for document {} failed with message {}", failed.documentId(), ex.getMessage());
    }
}
