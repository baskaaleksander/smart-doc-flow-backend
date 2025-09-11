package com.baskaaleksander.smartdocflowbackend.dto.response;

import com.baskaaleksander.smartdocflowbackend.enums.ReviewStatus;

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
