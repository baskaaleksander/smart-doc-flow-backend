package com.baskaaleksander.smartdocflowbackend.modules.documents;

import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.spring.SpringDataDocumentRepository;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.Document;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.DocumentQueryPort;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.EmbeddingTaskConsumerPort;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.FileStoragePort;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.OcrTaskConsumerPort;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.persistence.entity.NotificationEntity;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.persistence.spring.SpringDataNotificationRepository;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.model.NotificationType;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.AuthTestUtils;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.IntegrationTestBase;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.TestDataSeeder;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.TestDataUtils;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.entity.UserEntity;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.spring.SpringDataUserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DocumentsIntegrationTest extends IntegrationTestBase {

    @TestConfiguration
    static class OverrideMessagingConsumers {

        @Bean
        @Primary
        OcrTaskConsumerPort ocrTaskConsumerPort() {
            return mock(OcrTaskConsumerPort.class);
        }

        @Bean
        @Primary
        EmbeddingTaskConsumerPort embeddingTaskConsumerPort() {
            return mock(EmbeddingTaskConsumerPort.class);
        }

        @Bean
        @Primary
        FileStoragePort fileStoragePort() {
            FileStoragePort storagePort = mock(FileStoragePort.class);
            doNothing().when(storagePort).upload(any(), anyString(), anyString(), anyLong());
            doNothing().when(storagePort).delete(anyString());
            when(storagePort.getPresignedUrl(anyString(), anyString(), anyLong()))
                    .thenReturn("http://localhost/presigned");
            return storagePort;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthTestUtils auth;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataSeeder.TestSeeder seeder;

    @Autowired
    private TestDataUtils dataUtils;

    @Autowired
    private SpringDataDocumentRepository documentRepository;

    @Autowired
    private SpringDataNotificationRepository notificationRepository;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private DocumentQueryPort documentQueryPort;

    @BeforeAll
    void init() {
        seeder.seedAccountsIfNotExists();
    }

    @Test
    void upload_validPdf_returns201() throws Exception {
        String accessToken = auth.loginAndGetAccessToken("user", "User#12345");
        Set<UUID> existingDocumentIds = documentRepository.findAll().stream()
                .map(entity -> entity.getId())
                .collect(Collectors.toSet());
        Set<UUID> existingNotificationIds = notificationRepository.findAll().stream()
                .map(NotificationEntity::getId)
                .collect(Collectors.toSet());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.pdf",
                "application/pdf",
                "Test PDF content".getBytes(StandardCharsets.UTF_8)
        );

        MvcResult mvcResult = mockMvc.perform(multipart("/documents/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.filename").value("sample.pdf"))
                .andExpect(jsonPath("$.status").value("UPLOADED"))
                .andReturn();

        JsonNode body = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        UUID documentId = UUID.fromString(body.get("id").asText());

        Document persisted = documentQueryPort.getDocumentById(documentId)
                .orElseThrow(() -> new IllegalStateException("Document not found in repository"));
        assertThat(persisted.getStatus()).isEqualTo(DocumentStatus.UPLOADED);

        UserEntity user = userRepository.findByUsername("user")
                .orElseThrow(() -> new IllegalStateException("Seed user not found"));
        assertThat(persisted.getOwner().id()).isEqualTo(user.getId());

        Set<UUID> newDocumentIds = documentRepository.findAll().stream()
                .map(entity -> entity.getId())
                .filter(id -> !existingDocumentIds.contains(id))
                .collect(Collectors.toSet());
        assertThat(newDocumentIds).contains(documentId);

        var newNotifications = notificationRepository.findAll().stream()
                .filter(entity -> !existingNotificationIds.contains(entity.getId()))
                .toList();
        assertThat(newNotifications).hasSize(1);
        NotificationEntity notification = newNotifications.get(0);
        assertThat(notification.getUsername()).isEqualTo("user");
        assertThat(notification.getType()).isEqualTo(NotificationType.DOCUMENT_UPLOADED);
        assertThat(notification.isRead()).isFalse();
    }

    @Test
    void upload_invalidMimeOrUnauthorized() throws Exception {
        uploadInvalidMime();
        uploadMissingToken();
    }

    private void uploadInvalidMime() throws Exception {
        String accessToken = auth.loginAndGetAccessToken("user", "User#12345");
        long documentsBefore = documentRepository.count();
        long notificationsBefore = notificationRepository.count();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                "plain text".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/documents/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());

        assertThat(documentRepository.count()).isEqualTo(documentsBefore);
        assertThat(notificationRepository.count()).isEqualTo(notificationsBefore);
    }

    private void uploadMissingToken() throws Exception {
        long documentsBefore = documentRepository.count();
        long notificationsBefore = notificationRepository.count();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.pdf",
                "application/pdf",
                "Test PDF content".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/documents/upload")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnauthorized());

        assertThat(documentRepository.count()).isEqualTo(documentsBefore);
        assertThat(notificationRepository.count()).isEqualTo(notificationsBefore);
    }

    @Test
    void list_documentsPagingAndReviewerFilter() throws Exception {
        String adminToken = auth.loginAndGetAccessToken("admin", "Admin#12345");
        String reviewerToken = auth.loginAndGetAccessToken("reviewer", "Reviewer#12345");
        String userToken = auth.loginAndGetAccessToken("user", "User#12345");

        UserEntity owner = userRepository.findByUsername("user")
                .orElseThrow(() -> new IllegalStateException("Seed user 'user' not found"));
        UserEntity reviewer = userRepository.findByUsername("reviewer")
                .orElseThrow(() -> new IllegalStateException("Seed user 'reviewer' not found"));

        Set<UUID> seededDocuments = new HashSet<>();
        try {
            seededDocuments.add(dataUtils.createDocumentWithStatus(owner, DocumentStatus.PROCESSED));
            seededDocuments.add(dataUtils.createDocumentWithStatus(owner, DocumentStatus.REVIEW_PENDING));
            UUID assignedDocId = dataUtils.createDocumentAssignedToReviewer(owner, reviewer, DocumentStatus.IN_REVIEW);
            seededDocuments.add(assignedDocId);

            mockMvc.perform(get("/documents/")
                            .param("page", "0")
                            .param("size", "10")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.content[?(@.id=='" + assignedDocId + "')]").exists());

            MvcResult reviewerResult = mockMvc.perform(get("/documents/")
                            .param("assignedToMe", "true")
                            .param("page", "0")
                            .param("size", "10")
                            .header("Authorization", "Bearer " + reviewerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[?(@.id=='" + assignedDocId + "')]").exists())
                    .andExpect(jsonPath("$.content[*].review.reviewer").value(everyItem(equalTo("reviewer"))))
                    .andReturn();

            JsonNode reviewerContent = objectMapper.readTree(reviewerResult.getResponse().getContentAsString())
                    .get("content");

            Set<UUID> reviewerDocumentIds = new HashSet<>();
            reviewerContent.forEach(node -> reviewerDocumentIds.add(UUID.fromString(node.get("id").asText())));
            assertThat(reviewerDocumentIds).contains(assignedDocId);

            mockMvc.perform(get("/documents/")
                            .param("page", "0")
                            .param("size", "10")
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden());
        } finally {
            dataUtils.deleteDocumentsWithRelations(seededDocuments);
        }
    }

    @Test
    void stats_roleMatrix() throws Exception {
        String adminToken = auth.loginAndGetAccessToken("admin", "Admin#12345");
        JsonNode baseStats = fetchStats(adminToken);

        long basePending = baseStats.get("pendingReview").asLong();
        long baseInReview = baseStats.get("inReview").asLong();
        long baseReviewed = baseStats.get("reviewed").asLong();
        long baseFailed = baseStats.get("failed").asLong();

        Set<UUID> seededDocuments = new HashSet<>();
        try {
            UserEntity owner = userRepository.findByUsername("user")
                    .orElseThrow(() -> new IllegalStateException("Seed user 'user' not found"));

            seededDocuments.add(dataUtils.createDocumentWithStatus(owner, DocumentStatus.PROCESSED));
            seededDocuments.add(dataUtils.createDocumentWithStatus(owner, DocumentStatus.REVIEW_PENDING));
            seededDocuments.add(dataUtils.createDocumentWithStatus(owner, DocumentStatus.IN_REVIEW));
            seededDocuments.add(dataUtils.createDocumentWithStatus(owner, DocumentStatus.OCR_FAILED));
            seededDocuments.add(dataUtils.createDocumentWithStatus(owner, DocumentStatus.EMBED_FAILED));

            mockMvc.perform(get("/documents/stats")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pendingReview").value(basePending + 1))
                    .andExpect(jsonPath("$.inReview").value(baseInReview + 1))
                    .andExpect(jsonPath("$.reviewed").value(baseReviewed))
                    .andExpect(jsonPath("$.failed").value(baseFailed + 2));
        } finally {
            dataUtils.deleteDocumentsWithRelations(seededDocuments);
        }

        String userToken = auth.loginAndGetAccessToken("user", "User#12345");
        mockMvc.perform(get("/documents/stats")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    private JsonNode fetchStats(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/documents/stats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

}
