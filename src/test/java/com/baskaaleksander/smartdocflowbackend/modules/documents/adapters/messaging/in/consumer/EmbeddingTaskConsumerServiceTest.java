package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.messaging.in.consumer;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.event.EmbedTask;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.Chunk;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentOcrResult;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.VectorDocument;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.*;
import com.google.gson.JsonParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbeddingTaskConsumerServiceTest {

    @Mock
    private ChunkerServicePort chunkerService;
    @Mock
    private FileStoragePort fileStoragePort;
    @Mock
    private DocumentOcrResultQueryPort documentOcrResultQueryPort;
    @Mock
    private DocumentCommandPort documentCommandPort;
    @Mock
    private VectorIndexPort vectorIndexPort;

    @InjectMocks
    private EmbeddingTaskConsumerService service;

    @Test
    void handle_success_indexesChunks_updatesStatusAndPageCount() {
        UUID docId = UUID.randomUUID();
        DocumentOcrResult ocr = new DocumentOcrResult();
        ocr.setDocumentId(docId);
        ocr.setStorageKey("s3://bucket/ocr.json");

        when(documentOcrResultQueryPort.getOcrByDocId(docId)).thenReturn(Optional.of(ocr));
        when(fileStoragePort.getJsonFileValue("s3://bucket/ocr.json"))
                .thenReturn("{\"pages\":[{\"page\":1,\"text\":\"One\"},{\"page\":2,\"text\":\"Two\"}]}");

        Chunk c11 = new Chunk(docId, 1, 0, 10, "One-ChunkA");
        Chunk c12 = new Chunk(docId, 1, 11, 20, "One-ChunkB");
        Chunk c21 = new Chunk(docId, 2, 21, 30, "Two-ChunkA");
        when(chunkerService.chunkPage("One", docId, 1)).thenReturn(List.of(c11, c12));
        when(chunkerService.chunkPage("Two", docId, 2)).thenReturn(List.of(c21));

        service.handle(new EmbedTask(docId));

        ArgumentCaptor<List<VectorDocument>> docsCaptor = ArgumentCaptor.forClass(List.class);
        verify(vectorIndexPort).addAll(docsCaptor.capture());
        List<VectorDocument> sent = docsCaptor.getValue();
        assertThat(sent).hasSize(3);
        assertThat(sent).extracting(VectorDocument::text)
                .containsExactlyInAnyOrder("One-ChunkA", "One-ChunkB", "Two-ChunkA");
        assertThat(sent).allSatisfy(vd -> {
            assertThat(vd.metadata().get("docId")).isEqualTo(docId.toString());
            assertThat(vd.metadata()).containsKeys("page", "startOffset", "endOffset");
        });

        verify(documentCommandPort).updateStatus(docId, DocumentStatus.PROCESSED);
        verify(documentCommandPort).updatePageCount(docId, 2);
    }

    @Test
    void handle_ocrMissing_throwsNotFound() {
        UUID docId = UUID.randomUUID();
        when(documentOcrResultQueryPort.getOcrByDocId(docId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handle(new EmbedTask(docId)))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(fileStoragePort, chunkerService, vectorIndexPort, documentCommandPort);
    }

    @Test
    void handle_badJson_throwsJsonParseException() {
        UUID docId = UUID.randomUUID();
        DocumentOcrResult ocr = new DocumentOcrResult();
        ocr.setDocumentId(docId);
        ocr.setStorageKey("s3://key");
        when(documentOcrResultQueryPort.getOcrByDocId(docId)).thenReturn(Optional.of(ocr));
        when(fileStoragePort.getJsonFileValue("s3://key")).thenReturn("{not-json");

        assertThatThrownBy(() -> service.handle(new EmbedTask(docId)))
                .isInstanceOf(JsonParseException.class);

        verifyNoInteractions(chunkerService, vectorIndexPort);
        verify(documentCommandPort, never()).updateStatus(any(), any());
        verify(documentCommandPort, never()).updatePageCount(any(), anyInt());
    }
}