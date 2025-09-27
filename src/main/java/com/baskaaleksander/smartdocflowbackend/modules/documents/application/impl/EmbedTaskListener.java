package com.baskaaleksander.smartdocflowbackend.modules.documents.application.impl;

import com.baskaaleksander.smartdocflowbackend.common.config.QueueConfig;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.EmbedTask;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class EmbedTaskListener {
    private final EmbeddingService embeddingService;
    private final JobStatusService jobStatusService;

    public EmbedTaskListener(EmbeddingService embeddingService, JobStatusService jobStatusService) {
        this.embeddingService = embeddingService;
        this.jobStatusService = jobStatusService;
    }

    @RabbitListener(queues = QueueConfig.EMBED_QUEUE)
    public void handle(EmbedTask task) {
        jobStatusService.markInProgressEmbed(task.documentId());
        embeddingService.ingestDocument(task.documentId());
        jobStatusService.markProcessed(task.documentId());
    }

}
