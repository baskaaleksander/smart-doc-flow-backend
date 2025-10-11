package com.baskaaleksander.smartdocflowbackend.modules.documents.api.dto;

public record DocumentStatsResponse(
        Long pendingReview,
        Long inReview,
        Long reviewed,
        Long failed
) {
}
