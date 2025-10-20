package com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor @NoArgsConstructor
@Getter @Setter
public class DocumentReviewBasic {
    private UUID id;
    private String reviewer;
    private UUID reviewerId;
    private UUID documentId;
    private List<UUID> reviewEventIds;
    private String comment;
    private String status;
    private Instant updatedAt;
}
