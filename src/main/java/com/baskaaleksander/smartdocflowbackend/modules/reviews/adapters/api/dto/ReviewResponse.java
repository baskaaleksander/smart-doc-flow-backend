package com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.api.dto;

import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewStatus;

import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID documentId,
        ReviewStatus status,
        UUID reviewerId,
        String comment,
        Instant createdAt,
        Instant updatedAt,
        int version
        ) {
}
