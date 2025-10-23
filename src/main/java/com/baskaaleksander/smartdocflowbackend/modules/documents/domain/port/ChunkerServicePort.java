package com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.Chunk;

import java.util.List;
import java.util.UUID;

public interface ChunkerServicePort {
    List<Chunk> chunkPage(String rawText, UUID documentId, int page);
}
