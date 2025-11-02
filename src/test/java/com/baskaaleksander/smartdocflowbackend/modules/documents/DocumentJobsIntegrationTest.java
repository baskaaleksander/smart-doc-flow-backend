package com.baskaaleksander.smartdocflowbackend.modules.documents;

import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.messaging.in.EmbeddingRabbitListenerAdapter;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.messaging.in.OcrRabbitListenerAdapter;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.Document;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.Image;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.ChatCompletionPort;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.DocumentCommandPort;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.DocumentQueryPort;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.EmbeddingTaskPublisherPort;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.FileStoragePort;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.OcrEnginePort;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.OcrTaskPublisherPort;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.PdfRendererPort;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.VectorIndexPort;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.AuthTestUtils;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.IntegrationTestBase;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.TestDataSeeder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DocumentJobsIntegrationTest extends IntegrationTestBase {

    @TestConfiguration
    static class PipelineTestConfig {

        private static final AtomicBoolean ocrShouldFail = new AtomicBoolean(false);

        @Bean
        @Primary
        FileStoragePort inMemoryFileStoragePort() {
            return new InMemoryFileStoragePort();
        }

        @Bean
        @Primary
        PdfRendererPort pdfRendererPort() {
            return (file, dpi) -> List.of(
                    new Image("image-page-1".getBytes(StandardCharsets.UTF_8), "image/png", 1),
                    new Image("image-page-2".getBytes(StandardCharsets.UTF_8), "image/png", 2)
            );
        }

        @Bean
        @Primary
        OcrEnginePort ocrEnginePort() {
            return images -> {
                if (ocrShouldFail.get()) {
                    throw new RuntimeException("Simulated OCR failure");
                }
                return """
                        {
                          "pages": [
                            {"page": 1, "text": "First page. Important content."},
                            {"page": 2, "text": "Second page. More insights."}
                          ]
                        }
                        """;
            };
        }

        @Bean
        @Primary
        VectorIndexPort vectorIndexPort() {
            return docs -> { /* no-op for deterministic tests */ };
        }

        @Bean
        @Primary
        ChatCompletionPort chatCompletionPort() {
            return mock(ChatCompletionPort.class);
        }

        @Bean
        @Primary
        TaskExecutor pipelineTaskExecutor() {
            SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("pipeline-test-");
            return executor;
        }

        @Bean
        @Primary
        OcrTaskPublisherPort ocrTaskPublisherPort(OcrRabbitListenerAdapter listener, TaskExecutor pipelineTaskExecutor) {
            return task -> pipelineTaskExecutor.execute(() -> listener.onTask(task));
        }

        @Bean
        @Primary
        EmbeddingTaskPublisherPort embeddingTaskPublisherPort(EmbeddingRabbitListenerAdapter listener, TaskExecutor pipelineTaskExecutor) {
            return task -> pipelineTaskExecutor.execute(() -> listener.onTask(task));
        }

        @Bean
        @Primary
        RecordingDocumentCommandPort recordingDocumentCommandPort(
                @Qualifier("documentJpaAdapter") DocumentCommandPort delegate
        ) {
            return new RecordingDocumentCommandPort(delegate);
        }

        static void setOcrFailure(boolean fail) {
            ocrShouldFail.set(fail);
        }
    }

    static class RecordingDocumentCommandPort implements DocumentCommandPort {

        private final DocumentCommandPort delegate;
        private final Map<UUID, List<DocumentStatus>> statusHistory = new ConcurrentHashMap<>();
        private final Map<UUID, List<Integer>> pageCounts = new ConcurrentHashMap<>();

        RecordingDocumentCommandPort(DocumentCommandPort delegate) {
            this.delegate = delegate;
        }

        void clear() {
            statusHistory.clear();
            pageCounts.clear();
        }

        @Override
        public void deleteById(UUID documentId) {
            delegate.deleteById(documentId);
        }

        @Override
        public Document save(Document document) {
            return delegate.save(document);
        }

        @Override
        public void updateStatus(UUID documentId, DocumentStatus status) {
            statusHistory.computeIfAbsent(documentId, id -> Collections.synchronizedList(new ArrayList<>())).add(status);
            delegate.updateStatus(documentId, status);
        }

        @Override
        public void updatePageCount(UUID documentId, int count) {
            pageCounts.computeIfAbsent(documentId, id -> Collections.synchronizedList(new ArrayList<>())).add(count);
            delegate.updatePageCount(documentId, count);
        }

        public List<DocumentStatus> getStatuses(UUID documentId) {
            List<DocumentStatus> statuses = statusHistory.get(documentId);
            return statuses == null ? List.of() : List.copyOf(statuses);
        }

        public List<Integer> getPageCounts(UUID documentId) {
            List<Integer> counts = pageCounts.get(documentId);
            return counts == null ? List.of() : List.copyOf(counts);
        }
    }

    private static class InMemoryFileStoragePort implements FileStoragePort {
        private final Map<String, byte[]> storage = new ConcurrentHashMap<>();

        @Override
        public void upload(InputStream inputStream, String key, String contentType, long size) {
            try {
                byte[] bytes = inputStream.readAllBytes();
                storage.put(key, bytes);
            } catch (IOException e) {
                throw new RuntimeException("Failed to store key " + key, e);
            }
        }

        @Override
        public void delete(String key) {
            storage.remove(key);
        }

        @Override
        public String getPresignedUrl(String storageKey, String mime, Long duration) {
            return "http://localhost/mock/" + storageKey;
        }

        @Override
        public String getJsonFileValue(String storageKey) {
            byte[] bytes = storage.get(storageKey);
            if (bytes == null) {
                throw new IllegalStateException("No data stored under key " + storageKey);
            }
            return new String(bytes, StandardCharsets.UTF_8);
        }

        @Override
        public File getPdfFile(String storageKey) {
            byte[] bytes = storage.get(storageKey);
            if (bytes == null) {
                throw new IllegalStateException("No PDF stored under key " + storageKey);
            }
            try {
                File temp = File.createTempFile("pdf-", ".pdf");
                Files.write(temp.toPath(), bytes);
                temp.deleteOnExit();
                return temp;
            } catch (IOException e) {
                throw new RuntimeException("Failed to create temp file for key " + storageKey, e);
            }
        }
    }

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

    @BeforeAll
    void seedUsersAndData() {
        seeder.seedAccountsIfNotExists();
    }

    @Autowired
    private TestDataSeeder.TestSeeder seeder;

    @Test
    void pipeline_happyPath() throws Exception {
        PipelineTestConfig.setOcrFailure(false);
        recordingDocumentCommandPort.clear();

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
        UUID documentId = UUID.fromString(body.get("id").asText());

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
            UUID documentId = UUID.fromString(body.get("id").asText());

            Awaitility.await()
                    .atMost(Duration.ofSeconds(10))
                    .pollInterval(Duration.ofMillis(100))
                    .untilAsserted(() -> assertThat(recordingDocumentCommandPort.getStatuses(documentId))
                            .contains(DocumentStatus.OCR_FAILED));

            Document doc = documentQueryPort.getDocumentById(documentId)
                    .orElseThrow(() -> new IllegalStateException("Document not found after OCR failure"));

            assertThat(doc.getStatus()).isEqualTo(DocumentStatus.OCR_FAILED);

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
}
