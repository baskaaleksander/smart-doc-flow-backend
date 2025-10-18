package com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.DocumentRepository;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.port.DocumentCommandPort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class DocumentJpaAdapter implements DocumentCommandPort {

    private final DocumentRepository documentRepo;

    public DocumentJpaAdapter(DocumentRepository documentRepo) {
        this.documentRepo = documentRepo;
    }

    @Override
    @Transactional
    public void updateStatus(UUID documentId, String status) {
        documentRepo.updateStatus(documentId, DocumentStatus.fromString(status));
    }
}
