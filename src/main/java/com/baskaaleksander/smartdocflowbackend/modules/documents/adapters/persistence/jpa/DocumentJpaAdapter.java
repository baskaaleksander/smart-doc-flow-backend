package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.jpa;

import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.Document;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentReviewBasic;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.DocumentCommandPort;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.DocumentQueryPort;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.view.DocumentStatusCount;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class DocumentJpaAdapter implements DocumentCommandPort, DocumentQueryPort {
    @Override
    public Document save(Document document) {
        return null;
    }

    @Override
    public void attachReview(DocumentReviewBasic review) {

    }

    @Override
    public void updateStatus(UUID documentId, DocumentStatus status) {

    }

    @Override
    public Optional<Document> getDocumentById(UUID documentId) {
        return Optional.empty();
    }

    @Override
    public Optional<String> getOwnerUsernameById(UUID documentId) {
        return Optional.empty();
    }

    @Override
    public Optional<Document> findByIdWithReview(UUID documentId) {
        return Optional.empty();
    }

    @Override
    public PagingResult<Document> findAllByOwner(UUID ownerId, PaginationRequest request) {
        return null;
    }

    @Override
    public PagingResult<Document> findAllByReviewer(UUID reviewerId, PaginationRequest request) {
        return null;
    }

    @Override
    public PagingResult<Document> findAll(PaginationRequest request) {
        return null;
    }

    @Override
    public List<DocumentStatusCount> countDocumentsByStatus() {
        return List.of();
    }

    @Override
    public Set<Document> findAllByIdIn(Set<UUID> ids) {
        return Set.of();
    }
}
