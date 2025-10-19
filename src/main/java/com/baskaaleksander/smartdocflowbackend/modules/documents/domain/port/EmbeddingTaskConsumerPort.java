package com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.event.EmbedTask;

public interface EmbeddingTaskConsumerPort {
    void handle(EmbedTask task);
}
