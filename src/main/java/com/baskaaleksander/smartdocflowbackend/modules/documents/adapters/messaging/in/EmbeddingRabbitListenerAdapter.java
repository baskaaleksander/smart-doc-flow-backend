package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.messaging.in;

import com.baskaaleksander.smartdocflowbackend.common.config.QueueConfig;
import com.baskaaleksander.smartdocflowbackend.common.exception.EmbeddingTaskFailedException;
import com.baskaaleksander.smartdocflowbackend.common.logging.LoggingPort;
import com.baskaaleksander.smartdocflowbackend.modules.documents.application.job.JobStatusService;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.event.EmbedTask;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.EmbeddingTaskConsumerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmbeddingRabbitListenerAdapter {

    private final EmbeddingTaskConsumerPort consumer;
    private final JobStatusService jobStatusService;
    private final LoggingPort log;

    @RabbitListener(queues = QueueConfig.EMBED_QUEUE)
    public void onTask(EmbedTask task) {
        jobStatusService.markInProgressEmbed(task.documentId());
        try {
            consumer.handle(task);
            jobStatusService.markReviewPending(task.documentId());
        } catch (EmbeddingTaskFailedException ex) {
            jobStatusService.markFailedEmbed(task.documentId());
            log.warn("Embedding task failed for document " + task.documentId() + " – " + ex.getMessage());
        }
    }
}
