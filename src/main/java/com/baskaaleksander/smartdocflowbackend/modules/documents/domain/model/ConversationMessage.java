package com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor @NoArgsConstructor
@Getter @Setter
public class ConversationMessage {
    private UUID id;
    private ConversationSide side;
    private String content;
    private String fingerprint;
    private UUID conversationId;
    private UUID documentId;
    private UUID userId;
    private Instant createdAt;
}
