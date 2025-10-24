package com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence;

import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.spring.SpringDataDocumentRepository;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.port.DocumentCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReviewDocumentJpaAdapter implements DocumentCommandPort {

    private final SpringDataDocumentRepository documentRepo;

    @Override
    @Transactional
    public void updateStatus(UUID documentId, String status) {
        documentRepo.updateStatus(documentId, DocumentStatus.fromString(status));
    }
}
