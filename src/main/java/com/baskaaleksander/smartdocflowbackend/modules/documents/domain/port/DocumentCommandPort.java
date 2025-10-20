package com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.Document;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentStatus;

import java.util.UUID;

public interface DocumentCommandPort {
    Document save(Document document);
    void updateStatus(UUID documentId, DocumentStatus status);

}
