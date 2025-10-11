package com.baskaaleksander.smartdocflowbackend.modules.documents.api.dto;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.DocumentStatus;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse (
        UUID id,
        String filename,
        String mime,
        double size,
        int pageSize,
        DocumentOwnerBasicInfo owner,
        DocumentReviewBasicInfo review,
        DocumentStatus status,
        Instant createdAt
) {
}
