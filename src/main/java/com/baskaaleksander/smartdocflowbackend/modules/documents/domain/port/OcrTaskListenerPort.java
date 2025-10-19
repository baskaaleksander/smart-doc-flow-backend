package com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.event.OcrTask;

public interface OcrTaskListenerPort {
    void handle(OcrTask task);
}
