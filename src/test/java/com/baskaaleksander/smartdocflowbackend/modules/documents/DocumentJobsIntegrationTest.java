package com.baskaaleksander.smartdocflowbackend.modules.documents;

import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.entity.DocumentOcrResultEntity;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.spring.SpringDataDocumentOcrResultRepository;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.Document;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.DocumentQueryPort;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.FileStoragePort;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.AuthTestUtils;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.IntegrationTestBase;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.TestDataSeeder;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.pipeline.PipelineTestConfig;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.pipeline.RecordingDocumentCommandPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(PipelineTestConfig.class)
class DocumentJobsIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthTestUtils auth;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DocumentQueryPort documentQueryPort;

    @Autowired
    private RecordingDocumentCommandPort recordingDocumentCommandPort;

    @Autowired
    private SpringDataDocumentOcrResultRepository documentOcrResultRepository;

    @Autowired
    private FileStoragePort fileStoragePort;

    @Autowired
    private TestDataSeeder.TestSeeder seeder;

    @BeforeAll
    void seedUsersAndData() {
        seeder.seedAccountsIfNotExists();
    }

    @Test
    void pipeline_happyPath() throws Exception {
        PipelineTestConfig.setOcrFailure(false);
        recordingDocumentCommandPort.clear();

        UUID documentId = uploadDocument();

        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> assertThat(recordingDocumentCommandPort.getStatuses(documentId))
                        .contains(DocumentStatus.REVIEW_PENDING));

        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    Document doc = documentQueryPort.getDocumentById(documentId)
                            .orElseThrow(() -> new IllegalStateException("Document not found after processing"));
                    assertThat(doc.getStatus()).isEqualTo(DocumentStatus.REVIEW_PENDING);
                    assertThat(doc.getPageSize()).isGreaterThan(0);
                });

        List<DocumentStatus> statuses = recordingDocumentCommandPort.getStatuses(documentId);
        assertThat(statuses).containsSubsequence(
                DocumentStatus.IN_PROGRESS_OCR,
                DocumentStatus.TEXT_READY,
                DocumentStatus.IN_PROGRESS_EMBED,
                DocumentStatus.PROCESSED,
                DocumentStatus.REVIEW_PENDING
        );

        List<Integer> pageCounts = recordingDocumentCommandPort.getPageCounts(documentId);
        assertThat(pageCounts).isNotEmpty();
        assertThat(pageCounts.get(pageCounts.size() - 1)).isEqualTo(2);

        recordingDocumentCommandPort.clear();
    }

    @Test
    void pipeline_ocrFailure() throws Exception {
        PipelineTestConfig.setOcrFailure(true);
        recordingDocumentCommandPort.clear();

        try {
            UUID documentId = uploadDocument();

            Awaitility.await()
                    .atMost(Duration.ofSeconds(15))
                    .pollInterval(Duration.ofMillis(100))
                    .untilAsserted(() -> {
                        Document doc = documentQueryPort.getDocumentById(documentId)
                                .orElseThrow(() -> new IllegalStateException("Document not found after OCR failure"));
                        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.OCR_FAILED);
                });

            List<DocumentStatus> statuses = recordingDocumentCommandPort.getStatuses(documentId);
            assertThat(statuses).contains(DocumentStatus.IN_PROGRESS_OCR);
            assertThat(statuses).contains(DocumentStatus.OCR_FAILED);
            assertThat(statuses).doesNotContain(
                    DocumentStatus.TEXT_READY,
                    DocumentStatus.IN_PROGRESS_EMBED,
                    DocumentStatus.PROCESSED,
                    DocumentStatus.REVIEW_PENDING
            );

            List<Integer> pageCounts = recordingDocumentCommandPort.getPageCounts(documentId);
            assertThat(pageCounts).isEmpty();
        } finally {
            PipelineTestConfig.setOcrFailure(false);
            recordingDocumentCommandPort.clear();
        }
    }

    @Test
    void pipeline_embedFailure() throws Exception {
        PipelineTestConfig.setEmbedFailure(true);
        recordingDocumentCommandPort.clear();

        try {
            UUID documentId = uploadDocument();

            Awaitility.await()
                    .atMost(Duration.ofSeconds(15))
                    .pollInterval(Duration.ofMillis(100))
                    .untilAsserted(() -> {
                        Document doc = documentQueryPort.getDocumentById(documentId)
                                .orElseThrow(() -> new IllegalStateException("Document not found after embedding failure"));
                        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.EMBED_FAILED);
                });

            List<DocumentStatus> statuses = recordingDocumentCommandPort.getStatuses(documentId);
            assertThat(statuses).containsSubsequence(
                    DocumentStatus.IN_PROGRESS_OCR,
                    DocumentStatus.TEXT_READY,
                    DocumentStatus.IN_PROGRESS_EMBED
            );
            assertThat(statuses).contains(DocumentStatus.EMBED_FAILED);
            assertThat(statuses).doesNotContain(
                    DocumentStatus.PROCESSED,
                    DocumentStatus.REVIEW_PENDING
            );

            List<Integer> pageCounts = recordingDocumentCommandPort.getPageCounts(documentId);
            assertThat(pageCounts).isEmpty();
        } finally {
            PipelineTestConfig.setEmbedFailure(false);
            recordingDocumentCommandPort.clear();
        }
    }

    @Test
    void ocrResult_persistedAndLinked() throws Exception {
        recordingDocumentCommandPort.clear();
        
        UUID documentId = uploadDocument();

        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    List<DocumentStatus> statuses = recordingDocumentCommandPort.getStatuses(documentId);
                    assertThat(statuses).contains(DocumentStatus.TEXT_READY);
                });

        DocumentOcrResultEntity ocrResult = documentOcrResultRepository.getOcrByDocId(documentId)
                .orElseThrow(() -> new IllegalStateException("OCR result not found for document " + documentId));

        assertThat(ocrResult).isNotNull();
        assertThat(ocrResult.getDocument().getId()).isEqualTo(documentId);
        assertThat(ocrResult.getStorageKey()).isEqualTo(documentId + "_ocr.json");

        String ocrContent = fileStoragePort.getJsonFileValue(ocrResult.getStorageKey());
        assertThat(ocrContent).isNotEmpty();
        assertThat(ocrContent).contains("pages");
        assertThat(ocrContent).contains("First page. Important content.");
        assertThat(ocrContent).contains("Second page. More insights.");

        recordingDocumentCommandPort.clear();
    }

    private UUID uploadDocument() throws Exception {
        String userToken = auth.loginAndGetAccessToken("user", "User#12345");

        byte[] pdfBytes = "fake pdf content".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.pdf",
                "application/pdf",
                pdfBytes
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/documents/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }
}
