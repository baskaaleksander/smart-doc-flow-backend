package com.baskaaleksander.smartdocflowbackend.modules.documents.application.embed;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.event.EmbedTask;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.Chunk;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.OcrResult;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.OcrResultPage;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.EmbeddingTaskConsumerPort;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.entity.DocumentOcrResultEntity;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.spring.SpringDataDocumentOcrResultRepository;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.spring.SpringDataDocumentRepository;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.FileStoragePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonParseException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class EmbeddingTaskConsumerService implements EmbeddingTaskConsumerPort {

    private final SpringDataDocumentOcrResultRepository documentOcrResultRepository;
    private final ObjectMapper MAPPER = new ObjectMapper();
    private final VectorStoreLoader vectorStoreLoader;
    private final ChunkerService chunkerService;
    private final SpringDataDocumentRepository documentRepository;

    private final FileStoragePort fileStoragePort;


    public EmbeddingTaskConsumerService(
            SpringDataDocumentOcrResultRepository documentOcrResultRepository,
            VectorStoreLoader vectorStoreLoader,
            ChunkerService chunkerService,
            SpringDataDocumentRepository documentRepository,
            FileStoragePort fileStoragePort
            ) {
        this.documentOcrResultRepository = documentOcrResultRepository;
        this.vectorStoreLoader = vectorStoreLoader;
        this.chunkerService = chunkerService;
        this.documentRepository = documentRepository;
        this.fileStoragePort = fileStoragePort;
    }

    @Transactional
    @Override
    public void handle(EmbedTask task) {

        UUID docId = task.documentId();

        DocumentOcrResultEntity ocrResult = documentOcrResultRepository.getOcrByDocId(docId).orElseThrow(() -> new ResourceNotFoundException("Ocr result not found"));

        String response = fileStoragePort.getJsonFileValue(ocrResult.getStorageKey());
        OcrResult result = null;

        try {
            result = MAPPER.readValue(response, OcrResult.class);
        } catch (IOException e) {
            throw new JsonParseException("Failed to parse OCR result");
        }

        List<Chunk> chunks = new ArrayList<>();

        for (OcrResultPage page : result.pages()) {
            chunks.addAll(chunkerService.chunkPage(page.text(),docId ,page.page()));
        }

        vectorStoreLoader.loadChunks(chunks);

        documentRepository.updateStatus(docId, DocumentStatus.PROCESSED);
    }
}
