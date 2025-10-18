package com.baskaaleksander.smartdocflowbackend.modules.documents.api.dto;

import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewStatus;

import java.time.Instant;
import java.util.UUID;

public record DocumentReviewBasicInfo(UUID id, String reviewer, UUID reviewerId, ReviewStatus status, Instant updatedAt) {
}
