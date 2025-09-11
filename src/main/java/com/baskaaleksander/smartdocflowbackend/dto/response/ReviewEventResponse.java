package com.baskaaleksander.smartdocflowbackend.dto.response;

import com.baskaaleksander.smartdocflowbackend.enums.ReviewEventType;
import com.baskaaleksander.smartdocflowbackend.model.User;

import java.time.Instant;
import java.util.UUID;

public record ReviewEventResponse(
        UUID id,
        ReviewEventType eventType,
        String comment,
        UUID reviewerId,
        Instant createdAt
) {
}
