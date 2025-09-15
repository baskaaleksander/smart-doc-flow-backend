package com.baskaaleksander.smartdocflowbackend.enums;

import java.util.Arrays;

public enum NotificationType {
    DOCUMENT_UPLOADED("document_uploaded"),
    DOCUMENT_PROCESSED("document_processed"),
    DOCUMENT_IN_REVIEW("document_in_review"),
    DOCUMENT_REVIEWED("document_reviewed");


    private final String value;

    NotificationType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static NotificationType fromString(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status cannot be null or blank");
        }

        return Arrays.stream(values())
                .filter(s -> s.value.equalsIgnoreCase(status))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown status " + status));
    }
}
