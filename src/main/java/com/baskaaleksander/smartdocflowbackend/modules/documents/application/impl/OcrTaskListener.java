package com.baskaaleksander.smartdocflowbackend.modules.documents.application.impl;

import com.baskaaleksander.smartdocflowbackend.common.config.QueueConfig;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.OcrTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class OcrTaskListener {
    private final OcrService ocrService;
    private final Logger log = LoggerFactory.getLogger(OcrTaskListener.class);

    public OcrTaskListener(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    @RabbitListener(queues = QueueConfig.OCR_QUEUE)
    public void handle(OcrTask task) {
        ocrService.runOcr(task.documentId());
    }

}
