package com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.port;

import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewEvent;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewEventType;

import java.util.Optional;
import java.util.UUID;

public interface ReviewEventQueryPort {
    Optional<ReviewEvent> getReviewEventById(UUID id);
    PagingResult<ReviewEvent> findByReviewId(PaginationRequest request, UUID reviewId);
    PagingResult<ReviewEvent> findByReviewIdWithType(PaginationRequest request, UUID reviewId, ReviewEventType eventType);
}
