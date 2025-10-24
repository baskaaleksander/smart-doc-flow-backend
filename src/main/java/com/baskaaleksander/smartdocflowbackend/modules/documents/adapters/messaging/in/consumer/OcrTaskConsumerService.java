package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.messaging.in.consumer;


import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.event.EmbedTask;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.event.OcrTask;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.Document;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentOcrResult;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.Image;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
public class OcrTaskConsumerService implements OcrTaskConsumerPort {

    private final EmbeddingTaskPublisherPort taskPublisher;
    private final FileStoragePort fileStoragePort;
    private final DocumentQueryPort documentQueryPort;
    private final DocumentCommandPort documentCommandPort;
    private final DocumentOcrResultCommandPort documentOcrResultCommandPort;
    private final OcrEnginePort ocrEnginePort;
    private final PdfRendererPort pdfRendererPort;


    @Autowired
    public OcrTaskConsumerService(
            EmbeddingTaskPublisherPort taskPublisher,
            FileStoragePort fileStoragePort,
            DocumentQueryPort documentQueryPort,
            DocumentCommandPort documentCommandPort,
            DocumentOcrResultCommandPort documentOcrResultCommandPort,
            OcrEnginePort ocrEnginePort,
            PdfRendererPort pdfRendererPort
            ) {
        this.taskPublisher = taskPublisher;
        this.fileStoragePort = fileStoragePort;
        this.documentQueryPort = documentQueryPort;
        this.documentCommandPort = documentCommandPort;
        this.documentOcrResultCommandPort = documentOcrResultCommandPort;
        this.ocrEnginePort = ocrEnginePort;
        this.pdfRendererPort = pdfRendererPort;
    }

    @Transactional
    @Override
    public void handle(OcrTask task) {

        UUID documentId = task.documentId();

        Document doc = documentQueryPort.getDocumentById(documentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Document with ID " + documentId + " not found"));


            String documentKey = doc.getStorageKey();

            if (documentKey == null) {
                throw new ResourceNotFoundException("Document " + documentId + " has no key");
            }
            File file = null;

            try {
                file = getPdfFromS3(documentKey);

                List<Image> images = pdfRendererPort.render(file, 300);

                String rawText = ocrEnginePort.extractText(images);

                String docOcrKey = saveJsonToS3(rawText, documentId);

                saveOcrResultToDb(docOcrKey, documentId);

                documentCommandPort.updateStatus(documentId, DocumentStatus.TEXT_READY);

                taskPublisher.publish(new EmbedTask(documentId));

            } catch (Exception e) {
                //TODO: change this
                throw new RuntimeException(e);
            }
    }

    private File getPdfFromS3(String documentKey) {
        return fileStoragePort.getPdfFile(documentKey);
    }

    private String saveJsonToS3(String raw, UUID docId) {

        byte[] bytes = raw.getBytes(StandardCharsets.UTF_8);
        String docKey = docId + "_ocr.json";
        InputStream is = new ByteArrayInputStream(bytes);

        fileStoragePort.upload(is, docKey, "application/json", bytes.length);

        return docKey;
    }

    private void saveOcrResultToDb(String documentKey, UUID documentId) {

        Document document = documentQueryPort.getDocumentById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        DocumentOcrResult ocrResult = new DocumentOcrResult();
        ocrResult.setDocumentId(document.getId());
        ocrResult.setStorageKey(documentKey);

        documentOcrResultCommandPort.save(ocrResult);
    }
}
