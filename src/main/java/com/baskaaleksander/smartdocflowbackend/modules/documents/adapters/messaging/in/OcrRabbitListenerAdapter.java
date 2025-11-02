package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.messaging.in;

import com.baskaaleksander.smartdocflowbackend.common.config.QueueConfig;
import com.baskaaleksander.smartdocflowbackend.common.exception.OcrTaskFailedException;
import com.baskaaleksander.smartdocflowbackend.common.logging.LoggingPort;
import com.baskaaleksander.smartdocflowbackend.modules.documents.application.job.JobStatusService;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.event.OcrTask;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.OcrTaskConsumerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OcrRabbitListenerAdapter {

    private final OcrTaskConsumerPort consumer;
    private final JobStatusService jobStatusService;
    private final LoggingPort log;

    @RabbitListener(queues = QueueConfig.OCR_QUEUE)
    public void onTask(OcrTask task) {
        jobStatusService.markInProgressOcr(task.documentId());
        try {
            consumer.handle(task);
            jobStatusService.markTextReady(task.documentId());
        } catch (OcrTaskFailedException ex) {
            jobStatusService.markFailedOcr(task.documentId());
            log.warn("OCR task failed for document" + task.documentId() + " – " + ex.getMessage());
        }
    }
}
