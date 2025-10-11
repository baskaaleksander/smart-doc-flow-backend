package com.baskaaleksander.smartdocflowbackend.modules.reviews.api.dto;

import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.ReviewEventType;

import java.time.Instant;
import java.util.UUID;

public record ReviewEventResponse(
        UUID id,
        ReviewEventType eventType,
        String comment,
        EventReviewerBasicInfo reviewer,
        UUID reviewId,
        Instant createdAt
) {
}
