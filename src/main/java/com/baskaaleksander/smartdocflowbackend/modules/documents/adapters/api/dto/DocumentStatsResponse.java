package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api.dto;

public record DocumentStatsResponse(
        Long pendingReview,
        Long inReview,
        Long reviewed,
        Long failed
) {
}
