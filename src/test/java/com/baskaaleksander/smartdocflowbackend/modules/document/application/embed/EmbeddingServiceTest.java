package com.baskaaleksander.smartdocflowbackend.modules.document.application.embed;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.modules.documents.application.embed.ChunkerService;
import com.baskaaleksander.smartdocflowbackend.modules.documents.application.embed.EmbeddingTaskConsumerService;
import com.baskaaleksander.smartdocflowbackend.modules.documents.application.embed.VectorStoreLoader;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.Chunk;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.entity.DocumentOcrResultEntity;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.spring.SpringDataDocumentOcrResultRepository;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.spring.SpringDataDocumentRepository;
import com.google.gson.JsonParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmbeddingServiceTest {

    @Mock
    private SpringDataDocumentOcrResultRepository documentOcrResultRepository;
    @Mock
    private S3Client s3Client;
    @Mock
    private VectorStoreLoader vectorStoreLoader;
    @Mock
    private ChunkerService chunkerService;
    @Mock
    private SpringDataDocumentRepository documentRepository;

    @InjectMocks
    private EmbeddingTaskConsumerService embeddingService;

    private DocumentOcrResultEntity ocrResult;
    private UUID docId;


    @BeforeEach
    void setUp() {
        docId = UUID.randomUUID();
        ocrResult = new DocumentOcrResultEntity();
        ocrResult.setId(UUID.randomUUID());
        ocrResult.setStorageKey(docId + "_ocr");
        ReflectionTestUtils.setField(embeddingService, "s3Bucket", "bucket");
    }

    private static ResponseInputStream<GetObjectResponse> s3Stream(byte[] bytes) {
        GetObjectResponse head = GetObjectResponse.builder()
                .contentLength((long) bytes.length)
                .contentType("application/json")
                .build();

        AbortableInputStream body = AbortableInputStream.create(new ByteArrayInputStream(bytes));
        return new ResponseInputStream<>(head, body);
    }


    @Test
    void ingestDocument_shouldLoadJson_chunkAllPages_storeVectors_andUpdateStatus() {
        when(documentOcrResultRepository.getOcrByDocId(docId)).thenReturn(Optional.of(ocrResult));

        String json = """
            {
              "pages": [
                { "page": 1, "text": "Hello page one." },
                { "page": 2, "text": "Second page here." }
              ]
            }
            """;
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(s3Stream(json.getBytes(StandardCharsets.UTF_8)));

        Chunk c1 = new Chunk(docId, 1, 0, 5, "Hello");
        Chunk c2 = new Chunk(docId, 2, 0, 6, "Second");
        when(chunkerService.chunkPage("Hello page one.", docId, 1)).thenReturn(List.of(c1));
        when(chunkerService.chunkPage("Second page here.", docId, 2)).thenReturn(List.of(c2));

        embeddingService.ingestDocument(docId);

        ArgumentCaptor<GetObjectRequest> getCap = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObject(getCap.capture());
        assertThat(getCap.getValue().bucket()).isEqualTo("bucket");
        assertThat(getCap.getValue().key()).isEqualTo(ocrResult.getStorageKey() + ".json");

        ArgumentCaptor<List<Chunk>> chunksCap = ArgumentCaptor.forClass(List.class);
        verify(vectorStoreLoader).loadChunks(chunksCap.capture());
        assertThat(chunksCap.getValue()).containsExactly(c1, c2);

        verify(documentRepository).updateStatus(docId, DocumentStatus.PROCESSED);
        verify(chunkerService).chunkPage("Hello page one.", docId, 1);
        verify(chunkerService).chunkPage("Second page here.", docId, 2);
    }

    @Test
    void ingestDocument_shouldThrow_whenOcrNotFound() {
        when(documentOcrResultRepository.getOcrByDocId(docId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> embeddingService.ingestDocument(docId))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(s3Client, vectorStoreLoader, chunkerService, documentRepository);
    }

    @Test
    void ingestDocument_shouldThrowJsonParse_onInvalidJson() {
        when(documentOcrResultRepository.getOcrByDocId(docId)).thenReturn(Optional.of(ocrResult));

        String invalid = "{ not-a-json ";
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(s3Stream(invalid.getBytes(StandardCharsets.UTF_8)));

        assertThatThrownBy(() -> embeddingService.ingestDocument(docId))
                .isInstanceOf(JsonParseException.class)
                .hasMessageContaining("Failed to parse OCR result");

        verifyNoInteractions(vectorStoreLoader, chunkerService);
        verify(documentRepository, never()).updateStatus(any(), any());
    }

    @Test
    void ingestDocument_shouldHandleEmptyPages_withoutCrashing() {
        when(documentOcrResultRepository.getOcrByDocId(docId)).thenReturn(Optional.of(ocrResult));

        String emptyPages = """
            {
              "pages": []
            }
            """;
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(s3Stream(emptyPages.getBytes(StandardCharsets.UTF_8)));

        embeddingService.ingestDocument(docId);

        verify(vectorStoreLoader).loadChunks(List.of());
        verify(documentRepository).updateStatus(docId, DocumentStatus.PROCESSED);
        verifyNoInteractions(chunkerService);
    }
}
