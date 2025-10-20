package com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentOcrResult;

import java.util.Optional;
import java.util.UUID;

public interface DocumentOcrResultQueryPort {
    Optional<DocumentOcrResult> getOcrByDocId(UUID documentId);
}
