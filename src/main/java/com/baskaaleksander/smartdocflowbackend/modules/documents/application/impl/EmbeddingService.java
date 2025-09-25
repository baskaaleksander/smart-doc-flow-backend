package com.baskaaleksander.smartdocflowbackend.modules.documents.application.impl;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.Chunk;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.OcrResult;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.OcrResultPage;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.DocumentOcrResult;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.DocumentOcrResultRepository;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.DocumentRepository;
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
public class EmbeddingService {

    private final DocumentOcrResultRepository documentOcrResultRepository;
    private final S3Client s3Client;
    private final ObjectMapper MAPPER = new ObjectMapper();
    private final VectorStoreLoader vectorStoreLoader;
    private final ChunkerService chunkerService;
    private final DocumentRepository documentRepository;

    @Value(value = "${minio.bucket.name}")
    private String s3Bucket;

    public EmbeddingService(
            DocumentOcrResultRepository documentOcrResultRepository,
            S3Client s3Client,
            VectorStoreLoader vectorStoreLoader,
            ChunkerService chunkerService,
            DocumentRepository documentRepository) {
        this.documentOcrResultRepository = documentOcrResultRepository;
        this.s3Client = s3Client;
        this.vectorStoreLoader = vectorStoreLoader;
        this.chunkerService = chunkerService;
        this.documentRepository = documentRepository;
    }

    @Transactional
    public void ingestDocument(UUID docId) {
        DocumentOcrResult ocrResult = documentOcrResultRepository.getOcrByDocId(docId).orElseThrow(() -> new ResourceNotFoundException("Ocr result not found"));

        System.out.println(ocrResult.getStorageKey());

        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(s3Bucket)
                .key(ocrResult.getStorageKey() + ".json")
                .responseContentType("application/json")
                .build();

        var response = s3Client.getObject(get);
        OcrResult result = null;

        try {
            String json = new String(response.readAllBytes(), StandardCharsets.UTF_8);
            result = MAPPER.readValue(json, OcrResult.class);
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
