package com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentOcrResult;

public interface DocumentOcrResultCommandPort {
    DocumentOcrResult save(DocumentOcrResult result);
}
