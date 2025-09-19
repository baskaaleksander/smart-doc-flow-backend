package com.baskaaleksander.smartdocflowbackend.modules.reviews.domain;

import java.util.Arrays;

public enum ReviewStatus {
    IN_PROGRESS("in_progress"),
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected");

    private final String value;

    ReviewStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ReviewStatus fromString(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status cannot be null or blank");
        }

        return Arrays.stream(values())
                .filter(s -> s.value.equalsIgnoreCase(status))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown status: " + status));
    }
}
