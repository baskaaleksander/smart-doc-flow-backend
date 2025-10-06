package com.baskaaleksander.smartdocflowbackend.modules.document.application.embed;

import com.baskaaleksander.smartdocflowbackend.modules.documents.application.embed.ChunkerService;
import com.baskaaleksander.smartdocflowbackend.modules.documents.application.embed.EmbeddingService;
import com.baskaaleksander.smartdocflowbackend.modules.documents.application.embed.VectorStoreLoader;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.Document;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.DocumentOcrResultRepository;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class EmbeddingServiceTest {

    @Mock
    private DocumentOcrResultRepository documentOcrResultRepository;
    @Mock
    private S3Client s3Client;
    @Mock
    private VectorStoreLoader vectorStoreLoader;
    @Mock
    private ChunkerService chunkerService;
    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private EmbeddingService embeddingService;

    private Document doc;
    private UUID docId;
    @BeforeEach
    void setUp() {
        docId = UUID.randomUUID();
        doc = new Document();
        doc.setId(docId);
        doc.setStorageKey(docId + "_ocr");
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
}
