package com.baskaaleksander.smartdocflowbackend.modules.document.application.ocr;

import com.baskaaleksander.smartdocflowbackend.common.exception.PdfProcessingException;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.exception.S3DownloadException;
import com.baskaaleksander.smartdocflowbackend.common.exception.S3UploadException;
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
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    void runOcr_shouldProcessAndPersist_happyPath() throws Exception {
        when(documentRepository.getDocumentById(docId)).thenReturn(Optional.of(doc));
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(s3PdfStream(makeOnePagePdfBytes()));
        stubChatModelReturningJson("{\"pages\":[{\"page\":1,\"text\":\"Hello OCR!\"}]}");
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().eTag("etag").build());
        when(documentOcrResultRepository.save(any(DocumentOcrResult.class))).thenAnswer(inv -> inv.getArgument(0));

        ocrService.runOcr(docId);

        ArgumentCaptor<GetObjectRequest> getCap = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObject(getCap.capture());
        assertThat(getCap.getValue().bucket()).isEqualTo("bucket");
        assertThat(getCap.getValue().key()).isEqualTo(doc.getStorageKey());

        verify(chatModel).call(any(Prompt.class));

        ArgumentCaptor<PutObjectRequest> putCap = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(putCap.capture(), any(RequestBody.class));
        assertThat(putCap.getValue().bucket()).isEqualTo("bucket");
        assertThat(putCap.getValue().key()).isEqualTo(docId + "_ocr.json");
        assertThat(putCap.getValue().contentType()).isEqualTo("application/json");

        ArgumentCaptor<DocumentOcrResult> ocrCap = ArgumentCaptor.forClass(DocumentOcrResult.class);
        verify(documentOcrResultRepository).save(ocrCap.capture());
        assertThat(ocrCap.getValue().getDocument()).isEqualTo(doc);
        assertThat(ocrCap.getValue().getStorageKey()).isEqualTo(docId + "_ocr");

        verify(documentRepository).updateStatus(docId, DocumentStatus.TEXT_READY);
        verify(embedTaskPublisher).enqueue(docId);
    }

    @Test
    void runOcr_shouldThrow_whenDocumentMissing() {
        when(documentRepository.getDocumentById(docId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ocrService.runOcr(docId))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(s3Client, chatModel, documentOcrResultRepository, embedTaskPublisher);
    }

    @Test
    void runOcr_shouldWrap_s3Exception() {
        when(documentRepository.getDocumentById(docId)).thenReturn(Optional.of(doc));
        when(s3Client.getObject(any(GetObjectRequest.class))).thenThrow(S3DownloadException.class);

        assertThatThrownBy(() -> ocrService.runOcr(docId))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(S3DownloadException.class);

        verifyNoInteractions(chatModel, documentOcrResultRepository, embedTaskPublisher);
    }

    @Test
    void runOcr_shouldWrapPdfProcessingException_whenCorruptedPdf() {
        when(documentRepository.getDocumentById(docId)).thenReturn(Optional.of(doc));
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(s3PdfStream("NOT_A_PDF".getBytes()));

        assertThatThrownBy(() -> ocrService.runOcr(docId))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(PdfProcessingException.class);

        verifyNoInteractions(chatModel, documentOcrResultRepository, embedTaskPublisher);
    }

    @Test
    void runOcr_shouldWrapS3UploadException_whenJsonUploadFails() throws Exception {
        when(documentRepository.getDocumentById(docId)).thenReturn(Optional.of(doc));
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(s3PdfStream(makeOnePagePdfBytes()));
        stubChatModelReturningJson("{\"pages\":[]}");
        doThrow(S3Exception.builder().message("put failed").build())
                .when(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));

        assertThatThrownBy(() -> ocrService.runOcr(docId))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(S3UploadException.class);

        verifyNoInteractions(documentOcrResultRepository, embedTaskPublisher);
    }
}
