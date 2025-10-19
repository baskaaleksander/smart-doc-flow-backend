package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api.dto;

import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewStatus;

import java.time.Instant;
import java.util.UUID;

public record DocumentReviewBasicResponse(UUID id, String reviewer, UUID reviewerId, ReviewStatus status, Instant updatedAt) {
}
