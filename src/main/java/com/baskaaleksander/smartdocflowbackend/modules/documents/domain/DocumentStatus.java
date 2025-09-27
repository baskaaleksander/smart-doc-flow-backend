package com.baskaaleksander.smartdocflowbackend.modules.documents.domain;

public enum DocumentStatus {
    OCR_FAILED,
    EMBED_FAILED,
    UPLOADED,
    PROCESSED,
    TEXT_READY,
    REVIEW_PENDING,
    IN_REVIEW,
    REVIEWED
}
