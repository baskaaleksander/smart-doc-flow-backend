package com.baskaaleksander.smartdocflowbackend.modules.documents.application.job;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.DocumentCommandPort;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Transactional
public class JobStatusService {

    private final DocumentCommandPort documentCommandPort;

    public JobStatusService(DocumentCommandPort documentCommandPort) {
        this.documentCommandPort = documentCommandPort;
    }

    public void markFailedOcr(UUID documentId) {
        documentCommandPort.updateStatus(documentId, DocumentStatus.OCR_FAILED);
    }

    public void markFailedEmbed(UUID documentId) {
        documentCommandPort.updateStatus(documentId, DocumentStatus.EMBED_FAILED);

    }

    public void markTextReady(UUID documentId) {
        documentCommandPort.updateStatus(documentId, DocumentStatus.TEXT_READY);

    }

    public void markReviewPending(UUID documentId) {
        documentCommandPort.updateStatus(documentId, DocumentStatus.REVIEW_PENDING);

    }

    public void markInProgressOcr(UUID documentId) {
        documentCommandPort.updateStatus(documentId, DocumentStatus.IN_PROGRESS_OCR);
    }

    public void markInProgressEmbed(UUID documentId) {
        documentCommandPort.updateStatus(documentId, DocumentStatus.IN_PROGRESS_EMBED);
    }
}
