package com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model;

import java.util.UUID;

public record Chunk(UUID documentId, int page, int startOffset, int endOffset, String content) {
}
