package com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.event.OcrTask;

public interface OcrTaskPublisherPort {
    void publish(OcrTask task);
}
