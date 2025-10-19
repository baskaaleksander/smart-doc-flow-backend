package com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model;

import java.time.Instant;
import java.util.UUID;

public record DocumentReviewBasic(UUID id, String reviewer, UUID reviewerId, String status, Instant updatedAt) {
}
