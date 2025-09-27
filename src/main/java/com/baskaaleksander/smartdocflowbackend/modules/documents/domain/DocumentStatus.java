package com.baskaaleksander.smartdocflowbackend.modules.documents.domain;

public enum DocumentStatus {
    OCR_FAILED,
    EMBED_FAILED,
    IN_PROGRESS_OCR,
    IN_PROGRESS_EMBED,
    UPLOADED,
    PROCESSED,
    TEXT_READY,
    REVIEW_PENDING,
    IN_REVIEW,
    REVIEWED
}
