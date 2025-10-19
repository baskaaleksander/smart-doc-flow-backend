package com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.event.EmbedTask;

public interface EmbeddingTaskPublisherPort {
    void publish(EmbedTask task);
}
