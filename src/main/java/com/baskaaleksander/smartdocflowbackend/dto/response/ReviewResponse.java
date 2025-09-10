package com.baskaaleksander.smartdocflowbackend.dto.response;

import com.baskaaleksander.smartdocflowbackend.enums.ReviewStatus;

import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(
        Long id,
        UUID documentId,
        ReviewStatus status,
        Long reviewerId,
        String comment,
        Instant createdAt,
        Instant updatedAt,
        int version
        ) {
}
