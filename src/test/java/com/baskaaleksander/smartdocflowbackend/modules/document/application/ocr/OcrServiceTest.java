package com.baskaaleksander.smartdocflowbackend.modules.document.application.ocr;

import com.baskaaleksander.smartdocflowbackend.modules.documents.application.embed.EmbedTaskPublisher;
import com.baskaaleksander.smartdocflowbackend.modules.documents.application.ocr.OcrService;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.Document;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.DocumentOcrResult;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.DocumentOcrResultRepository;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.DocumentRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OcrServiceTest {

    @Mock
    private S3Client s3Client;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private OpenAiChatModel chatModel;
    @Mock
    private DocumentOcrResultRepository documentOcrResultRepository;
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

    private static byte[] makeOnePagePdfBytes() throws Exception {
        try (PDDocument pdf = new PDDocument()) {
            pdf.addPage(new PDPage());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            pdf.save(baos);
            return baos.toByteArray();
        }
    }

    private void stubChatModelReturningJson(String json) {
        ChatResponse resp = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
        when(resp.getResult().getOutput().getText()).thenReturn(json);

        when(chatModel.call(any(Prompt.class))).thenReturn(resp);
    }

    private static ResponseInputStream<GetObjectResponse> s3PdfStream(byte[] pdfBytes) {
        GetObjectResponse head = GetObjectResponse.builder()
                .contentLength((long) pdfBytes.length)
                .contentType("application/pdf")
                .build();

        AbortableInputStream body = AbortableInputStream.create(new ByteArrayInputStream(pdfBytes));
        return new ResponseInputStream<>(head, body);
    }


}
