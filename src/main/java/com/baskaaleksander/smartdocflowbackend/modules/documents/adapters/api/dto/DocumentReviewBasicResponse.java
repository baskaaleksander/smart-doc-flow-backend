package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api.dto;

import java.time.Instant;
import java.util.UUID;

public record DocumentReviewBasicResponse(UUID id, String reviewer, UUID reviewerId, String status, Instant updatedAt) {
}
