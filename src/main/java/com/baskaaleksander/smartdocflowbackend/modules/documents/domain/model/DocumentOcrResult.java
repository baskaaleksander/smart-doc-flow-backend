package com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor @NoArgsConstructor
@Getter @Setter
public class DocumentOcrResult {
    private UUID id;
    private String storageKey;
    private UUID documentId;
    private int version;
    private Instant createdAt;
}
