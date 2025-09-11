package com.baskaaleksander.smartdocflowbackend.dto.response;

import com.baskaaleksander.smartdocflowbackend.enums.DocumentStatus;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse (
        UUID id,
        String filename,
        String mime,
        double size,
        int pageSize,
        UUID ownerId,
        UUID reviewId,
        DocumentStatus status,
        Instant createdAt
) {
}
