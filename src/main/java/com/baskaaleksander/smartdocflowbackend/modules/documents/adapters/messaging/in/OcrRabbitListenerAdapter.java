package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.messaging.in;

import com.baskaaleksander.smartdocflowbackend.common.config.QueueConfig;
import com.baskaaleksander.smartdocflowbackend.modules.documents.application.job.JobStatusService;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.event.OcrTask;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.OcrTaskConsumerPort;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OcrRabbitListenerAdapter {

    private final OcrTaskConsumerPort consumer;
    private final JobStatusService jobStatusService;

    private OcrRabbitListenerAdapter(
            OcrTaskConsumerPort consumer,
            JobStatusService jobStatusService
            ) {
        this.consumer = consumer;
        this.jobStatusService = jobStatusService;
    }

    @RabbitListener(queues = QueueConfig.OCR_QUEUE)
    public void onTask(OcrTask task) {
        jobStatusService.markInProgressOcr(task.documentId());
        consumer.handle(task);
        jobStatusService.markTextReady(task.documentId());
    }
}
