package com.baskaaleksander.smartdocflowbackend.dto.response;

import com.baskaaleksander.smartdocflowbackend.enums.DocumentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentResponse (
        UUID id,
        String filename,
        String mime,
        double size,
        int pageSize,
        long ownerId,
        DocumentStatus status,
        LocalDateTime createdAt
) {
}
