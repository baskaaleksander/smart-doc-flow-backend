package com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence;

import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.spring.SpringDataDocumentRepository;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.port.ReviewDocumentCommandPort;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.port.ReviewDocumentQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReviewDocumentJpaAdapter implements ReviewDocumentCommandPort, ReviewDocumentQueryPort {

    private final SpringDataDocumentRepository documentRepo;

    @Override
    @Transactional
    public void updateStatus(UUID documentId, String status) {
        documentRepo.updateStatus(documentId, DocumentStatus.fromString(status));
    }

    @Override
    public Optional<UUID> getOwnerIdByReviewId(UUID reviewId) {
        return documentRepo.getOwnerIdByReviewId(reviewId);
    }
}
