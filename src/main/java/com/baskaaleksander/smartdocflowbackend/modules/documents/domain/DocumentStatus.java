package com.baskaaleksander.smartdocflowbackend.modules.documents.domain;

import java.util.Arrays;

public enum DocumentStatus {
    OCR_FAILED("ocr_failed"),
    EMBED_FAILED("embed_failed"),
    IN_PROGRESS_OCR("in_progress_ocr"),
    IN_PROGRESS_EMBED("in_progress_embed"),
    UPLOADED("uploaded"),
    PROCESSED("processed"),
    TEXT_READY("text_ready"),
    REVIEW_PENDING("review_pending"),
    IN_REVIEW("in_review"),
    REVIEWED("reviewed");

    private final String value;

    DocumentStatus(String value) { this.value = value; };

    public static DocumentStatus fromString(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status cannot be null or blank");
        }

        return Arrays.stream(values())
                .filter(s -> s.value.equalsIgnoreCase(status))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown status " + status));
    }
}
