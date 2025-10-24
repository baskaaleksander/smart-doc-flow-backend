package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.messaging.in;

import com.baskaaleksander.smartdocflowbackend.common.config.QueueConfig;
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

    @RabbitListener(queues = QueueConfig.EMBED_QUEUE)
    public void onTask(EmbedTask task) {
        jobStatusService.markInProgressEmbed(task.documentId());
        consumer.handle(task);
        jobStatusService.markReviewPending(task.documentId());
    }
}
