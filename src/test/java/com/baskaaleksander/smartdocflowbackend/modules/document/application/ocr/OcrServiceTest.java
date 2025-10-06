package com.baskaaleksander.smartdocflowbackend.modules.document.application.ocr;

import com.baskaaleksander.smartdocflowbackend.modules.documents.application.embed.EmbedTaskPublisher;
import com.baskaaleksander.smartdocflowbackend.modules.documents.application.embed.EmbeddingService;
import com.baskaaleksander.smartdocflowbackend.modules.documents.application.ocr.OcrService;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.Document;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.DocumentOcrResultRepository;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;

import java.time.Instant;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class OcrServiceTest {

    @Mock
    private S3Client s3Client;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private OpenAiChatModel openAiChatModel;
    @Mock
    private DocumentOcrResultRepository documentOcrResultRepository;
    @Mock
    private EmbeddingService embeddingService;
    @Mock
    private EmbedTaskPublisher embedTaskPublisher;

    @InjectMocks
    private OcrService ocrService;

    private UUID docId;
    private Document doc;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(ocrService, "bucket", "bucket");
        ReflectionTestUtils.setField(ocrService, "model", "gpt-4o");

        docId = UUID.randomUUID();
        doc = new Document();
        doc.setId(docId);
        doc.setCreatedAt(Instant.now());
        doc.setStatus(DocumentStatus.UPLOADED);
        doc.setStorageKey(docId + "_file.pdf");
    }
}
