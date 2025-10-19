package com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model;

import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewStatus;

import java.time.Instant;
import java.util.UUID;

public record DocumentReviewBasic(UUID id, String reviewer, UUID reviewerId, ReviewStatus status, Instant updatedAt) {
}
