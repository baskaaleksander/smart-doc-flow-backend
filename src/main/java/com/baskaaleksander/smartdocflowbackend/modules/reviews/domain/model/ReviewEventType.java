package com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model;

import java.util.Arrays;

public enum ReviewEventType {
    ASSIGNED("assigned"),
    RELEASED("released"),
    APPROVED("approved"),
    REJECTED("rejected"),
    COMMENT("comment");

    private final String value;

    ReviewEventType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ReviewEventType fromString(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status cannot be null or blank");
        }

        return Arrays.stream(values())
                .filter(s -> s.value.equalsIgnoreCase(status))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown status " + status));
    }
}
