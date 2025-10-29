package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.messaging.in.consumer;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.logging.LoggingPort;
import com.baskaaleksander.smartdocflowbackend.common.logging.Slf4jLoggingAdapter;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.event.EmbedTask;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.event.OcrTask;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.Document;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentOcrResult;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.Image;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OcrTaskConsumerService implements OcrTaskConsumerPort {

    private final EmbeddingTaskPublisherPort taskPublisher;
    private final FileStoragePort fileStoragePort;
    private final DocumentQueryPort documentQueryPort;
    private final DocumentCommandPort documentCommandPort;
    private final DocumentOcrResultCommandPort documentOcrResultCommandPort;
    private final OcrEnginePort ocrEnginePort;
    private final PdfRendererPort pdfRendererPort;
    private final LoggingPort logger;

    @Transactional
    @Override
    public void handle(OcrTask task) {
        UUID docId = task.documentId();
        long start = System.currentTimeMillis();

        logger.info("OCR_TASK START docId=" + Slf4jLoggingAdapter.shortId(docId));

        Document doc = documentQueryPort.getDocumentById(docId)
                .orElseThrow(() -> {
                    logger.warn("OCR_TASK FAILED reason=document_not_found docId=" + Slf4jLoggingAdapter.shortId(docId));
                    return new ResourceNotFoundException("Document with ID " + docId + " not found");
                });

        String documentKey = doc.getStorageKey();
        if (documentKey == null) {
            logger.warn("OCR_TASK FAILED reason=no_storage_key docId=" + Slf4jLoggingAdapter.shortId(docId));
            throw new ResourceNotFoundException("Document " + docId + " has no key");
        }

        File file;
        try {
            file = fileStoragePort.getPdfFile(documentKey);
            logger.info("OCR_TASK FETCH_SUCCESS docId=" + Slf4jLoggingAdapter.shortId(docId) + " key=" + documentKey);
        } catch (Exception e) {
            logger.error("OCR_TASK FAILED reason=fetch_pdf_error key=" + documentKey + " docId=" + Slf4jLoggingAdapter.shortId(docId), e);
            throw e;
        }

        try {
            List<Image> images = pdfRendererPort.render(file, 300);
            logger.info("OCR_TASK RENDER_SUCCESS docId=" + Slf4jLoggingAdapter.shortId(docId) + " pages=" + images.size());

            String rawText = ocrEnginePort.extractText(images);
            logger.info("OCR_TASK OCR_SUCCESS docId=" + Slf4jLoggingAdapter.shortId(docId) + " textLength=" + rawText.length());

            String docOcrKey = saveJsonToS3(rawText, docId);
            logger.info("OCR_TASK UPLOAD_SUCCESS docId=" + Slf4jLoggingAdapter.shortId(docId) + " key=" + docOcrKey);

            saveOcrResultToDb(docOcrKey, docId);
            documentCommandPort.updateStatus(docId, DocumentStatus.TEXT_READY);

            taskPublisher.publish(new EmbedTask(docId));
            long took = System.currentTimeMillis() - start;

            logger.info("OCR_TASK SUCCESS docId=" + Slf4jLoggingAdapter.shortId(docId) + " took=" + took + "ms");

        } catch (Exception e) {
            logger.error("OCR_TASK FAILED docId=" + Slf4jLoggingAdapter.shortId(docId)
                    + " reason=" + e.getMessage(), e);
            documentCommandPort.updateStatus(docId, DocumentStatus.OCR_FAILED);
        }
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