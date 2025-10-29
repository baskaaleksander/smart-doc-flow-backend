package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.messaging.in.consumer;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.logging.LoggingPort;
import com.baskaaleksander.smartdocflowbackend.common.logging.Slf4jLoggingAdapter;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.event.EmbedTask;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.*;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonParseException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmbeddingTaskConsumerService implements EmbeddingTaskConsumerPort {

    private final ObjectMapper MAPPER = new ObjectMapper();
    private final ChunkerServicePort chunkerService;
    private final FileStoragePort fileStoragePort;
    private final DocumentOcrResultQueryPort documentOcrResultQueryPort;
    private final DocumentCommandPort documentCommandPort;
    private final VectorIndexPort vectorIndexPort;
    private final LoggingPort logger;

    @Transactional
    @Override
    public void handle(EmbedTask task) {
        UUID docId = task.documentId();
        long start = System.currentTimeMillis();

        logger.info("EMBED_TASK START docId=" + Slf4jLoggingAdapter.shortId(docId));

        DocumentOcrResult ocrResult = documentOcrResultQueryPort.getOcrByDocId(docId)
                .orElseThrow(() -> {
                    logger.warn("EMBED_TASK FAILED reason=ocr_result_not_found docId=" + Slf4jLoggingAdapter.shortId(docId));
                    return new ResourceNotFoundException("Ocr result not found");
                });

        String response;
        try {
            response = fileStoragePort.getJsonFileValue(ocrResult.getStorageKey());
        } catch (Exception e) {
            logger.error("EMBED_TASK FAILED reason=failed_to_read_file key=" + ocrResult.getStorageKey() +
                    " docId=" + Slf4jLoggingAdapter.shortId(docId), e);
            throw e;
        }

        OcrResult result;
        try {
            result = MAPPER.readValue(response, OcrResult.class);
        } catch (IOException e) {
            logger.error("EMBED_TASK FAILED reason=invalid_json key=" + ocrResult.getStorageKey() +
                    " docId=" + Slf4jLoggingAdapter.shortId(docId), e);
            throw new JsonParseException("Failed to parse OCR result");
        }

        List<Chunk> chunks = new ArrayList<>();
        int pageCount = 0;

        for (OcrResultPage page : result.pages()) {
            List<Chunk> pageChunks = chunkerService.chunkPage(page.text(), docId, page.page());
            chunks.addAll(pageChunks);
            pageCount++;
        }

        logger.info("EMBED_TASK CHUNKING_SUCCESS docId=" + Slf4jLoggingAdapter.shortId(docId)
                + " pages=" + pageCount + " totalChunks=" + chunks.size());

        List<VectorDocument> docs = chunkToVectorDocument(chunks);

        try {
            vectorIndexPort.addAll(docs);
            logger.info("EMBED_TASK INDEXING_SUCCESS docId=" + Slf4jLoggingAdapter.shortId(docId)
                    + " vectors=" + docs.size());
        } catch (Exception e) {
            logger.error("EMBED_TASK FAILED reason=indexing_error docId=" + Slf4jLoggingAdapter.shortId(docId)
                    + " vectors=" + docs.size(), e);
            throw e;
        }

        documentCommandPort.updateStatus(docId, DocumentStatus.PROCESSED);
        documentCommandPort.updatePageCount(docId, pageCount);

        long took = System.currentTimeMillis() - start;
        logger.info("EMBED_TASK SUCCESS docId=" + Slf4jLoggingAdapter.shortId(docId)
                + " pages=" + pageCount + " chunks=" + chunks.size() + " took=" + took + "ms");
    }

    private List<VectorDocument> chunkToVectorDocument(List<Chunk> chunks) {
        return chunks.stream().map(c ->
                new VectorDocument(
                        c.content(),
                        Map.of(
                                "docId", c.documentId().toString(),
                                "page", c.page(),
                                "startOffset", c.startOffset(),
                                "endOffset", c.endOffset()
                        ))
        ).toList();
    }
}