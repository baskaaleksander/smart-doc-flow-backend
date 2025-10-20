package com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port;

import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.Document;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.view.DocumentStatusCount;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface DocumentQueryPort {
    Optional<Document> getDocumentById(UUID documentId);
    Optional<String> getOwnerUsernameById(UUID documentId);
    Optional<Document> findByIdWithReview(UUID documentId);
    PagingResult<Document> findAllByOwner(UUID ownerId, PaginationRequest request);
    PagingResult<Document> findAllByReviewer(UUID reviewerId, PaginationRequest request);
    PagingResult<Document> findAll(PaginationRequest request);
    List<DocumentStatusCount> countDocumentsByStatus();
    Set<Document> findAllByIdIn(Set<UUID> ids);
}
