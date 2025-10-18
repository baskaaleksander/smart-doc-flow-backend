package com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.port;

import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.Review;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewStatus;

import java.util.Optional;
import java.util.UUID;

public interface ReviewQueryPort {
    Optional<UUID> getReviewerIdByDocumentId(UUID documentId);
    Optional<Review> getReviewById(UUID id);
    PagingResult<Review> findAll(PaginationRequest request);
    PagingResult<Review> findByStatus(PaginationRequest request, ReviewStatus status);
}
