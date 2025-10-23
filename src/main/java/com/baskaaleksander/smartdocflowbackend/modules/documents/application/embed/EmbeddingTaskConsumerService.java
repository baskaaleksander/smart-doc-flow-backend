package com.baskaaleksander.smartdocflowbackend.modules.documents.application.embed;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.event.EmbedTask;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.*;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonParseException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class EmbeddingTaskConsumerService implements EmbeddingTaskConsumerPort {

    private final ObjectMapper MAPPER = new ObjectMapper();
    private final VectorStoreLoader vectorStoreLoader;

    private final ChunkerServicePort chunkerService;
    private final FileStoragePort fileStoragePort;
    private final DocumentOcrResultQueryPort documentOcrResultQueryPort;
    private final DocumentCommandPort documentCommandPort;


    public EmbeddingTaskConsumerService(
            VectorStoreLoader vectorStoreLoader,

            ChunkerServicePort chunkerService,
            FileStoragePort fileStoragePort,
            DocumentOcrResultQueryPort documentOcrResultQueryPort,
            DocumentCommandPort documentCommandPort
            ) {
        this.vectorStoreLoader = vectorStoreLoader;

        this.chunkerService = chunkerService;
        this.fileStoragePort = fileStoragePort;
        this.documentOcrResultQueryPort = documentOcrResultQueryPort;
        this.documentCommandPort = documentCommandPort;
    }

    @Transactional
    @Override
    public void handle(EmbedTask task) {

        UUID docId = task.documentId();

        DocumentOcrResult ocrResult = documentOcrResultQueryPort.getOcrByDocId(docId).orElseThrow(() -> new ResourceNotFoundException("Ocr result not found"));

        String response = fileStoragePort.getJsonFileValue(ocrResult.getStorageKey());
        OcrResult result = null;

        try {
            result = MAPPER.readValue(response, OcrResult.class);
        } catch (IOException e) {
            throw new JsonParseException("Failed to parse OCR result");
        }

        List<Chunk> chunks = new ArrayList<>();
        int pageCount = 0;

        for (OcrResultPage page : result.pages()) {
            chunks.addAll(chunkerService.chunkPage(page.text(),docId ,page.page()));
            pageCount++;
        }

        vectorStoreLoader.loadChunks(chunks);

        documentCommandPort.updateStatus(docId, DocumentStatus.PROCESSED);
        documentCommandPort.updatePageCount(docId, pageCount);
    }
}
