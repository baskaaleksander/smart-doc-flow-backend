package com.baskaaleksander.smartdocflowbackend.modules.documents.application;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.DocumentRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class JobStatusService {

    private final DocumentRepository documentRepository;

    public JobStatusService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @Transactional
    public void markFailedOcr(UUID documentId) {
        documentRepository.updateStatus(documentId, DocumentStatus.OCR_FAILED);
    }
    @Transactional
    public void markFailedEmbed(UUID documentId) {
        documentRepository.updateStatus(documentId, DocumentStatus.EMBED_FAILED);

    }
    @Transactional
    public void markTextReady(UUID documentId) {
        documentRepository.updateStatus(documentId, DocumentStatus.TEXT_READY);

    }
    @Transactional
    public void markReviewPending(UUID documentId) {
        documentRepository.updateStatus(documentId, DocumentStatus.REVIEW_PENDING);

    }
    @Transactional
    public void markInProgressOcr(UUID documentId) {
        documentRepository.updateStatus(documentId, DocumentStatus.IN_PROGRESS_OCR);
    }
    @Transactional
    public void markInProgressEmbed(UUID documentId) {
        documentRepository.updateStatus(documentId, DocumentStatus.IN_PROGRESS_EMBED);
    }
}
