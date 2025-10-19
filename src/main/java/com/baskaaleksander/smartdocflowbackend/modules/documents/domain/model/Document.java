package com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor @NoArgsConstructor
@Getter @Setter
public class Document {
    private UUID id;
    private String filename;
    private String mime;
    private double size;
    private String storageKey;
    private int pageSize;
    private DocumentStatus status;
    private Instant createdAt;
    private DocumentUserBasic owner;
    private DocumentReviewBasic review;
}
