package com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.api.dto;

import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewEventType;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewerBasic;

import java.time.Instant;
import java.util.UUID;

public record ReviewEventResponse(
        UUID id,
        ReviewEventType eventType,
        String comment,
        ReviewerBasic reviewer,
        UUID reviewId,
        Instant createdAt
) {
}
