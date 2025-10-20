package com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor @NoArgsConstructor
@Getter @Setter
public class DocumentReviewBasic {
    private UUID id;
    private String reviewer;
    private UUID reviewerId;
    private String status;
    private Instant updatedAt;
}
