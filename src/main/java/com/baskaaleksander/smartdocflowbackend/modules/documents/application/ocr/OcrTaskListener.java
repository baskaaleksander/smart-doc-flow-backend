package com.baskaaleksander.smartdocflowbackend.modules.documents.application.ocr;

import com.baskaaleksander.smartdocflowbackend.common.config.QueueConfig;
import com.baskaaleksander.smartdocflowbackend.modules.documents.application.JobStatusService;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.event.OcrTask;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class OcrTaskListener {
    private final OcrService ocrService;
    private final JobStatusService jobStatusService;

    public OcrTaskListener(OcrService ocrService, JobStatusService jobStatusService) {
        this.ocrService = ocrService;
        this.jobStatusService = jobStatusService;
    }

    @RabbitListener(queues = QueueConfig.OCR_QUEUE)
    public void handle(OcrTask task) {
        jobStatusService.markInProgressOcr(task.documentId());
        ocrService.runOcr(task.documentId());
        jobStatusService.markTextReady(task.documentId());
    }

}
