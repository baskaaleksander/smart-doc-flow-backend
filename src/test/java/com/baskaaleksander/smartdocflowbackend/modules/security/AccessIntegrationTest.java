package com.baskaaleksander.smartdocflowbackend.modules.security;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.ChatCompletionPort;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.FileStoragePort;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.VectorQueryPort;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.AuthTestUtils;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.IntegrationTestBase;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.TestDataSeeder;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.TestDataUtils;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.model.TestUser;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.entity.UserEntity;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.spring.SpringDataUserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccessIntegrationTest extends IntegrationTestBase {

    @TestConfiguration
    static class AccessTestConfig {
        @Bean
        @Primary
        FileStoragePort fileStoragePort() {
            FileStoragePort storagePort = mock(FileStoragePort.class);
            doNothing().when(storagePort).delete(anyString());
            return storagePort;
        }

        @Bean
        @Primary
        ChatCompletionPort chatCompletionPort() {
            return mock(ChatCompletionPort.class);
        }

        @Bean
        @Primary
        VectorQueryPort vectorQueryPort() {
            return mock(VectorQueryPort.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthTestUtils auth;

    @Autowired
    private TestDataSeeder.TestSeeder seeder;

    @Autowired
    private TestDataUtils dataUtils;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private ChatCompletionPort chatCompletionPort;

    @Autowired
    private VectorQueryPort vectorQueryPort;

    @BeforeAll
    void init() {
        seeder.seedAccountsIfNotExists();
    }

    @Test
    void guards_spotChecks() throws Exception {
        documentAccessSpotChecks();
        conversationAccessSpotChecks();
    }

    private void documentAccessSpotChecks() throws Exception {
        String ownerToken = auth.loginAndGetAccessToken("user", "User#12345");
        String adminToken = auth.loginAndGetAccessToken("admin", "Admin#12345");
        String reviewerToken = auth.loginAndGetAccessToken("reviewer", "Reviewer#12345");

        TestUser otherUser = dataUtils.createIsolatedUser("Other#12345");
        String otherToken = auth.loginAndGetAccessToken(otherUser.getUsername(), otherUser.getRawPassword());

        UserEntity owner = userRepository.findByUsername("user")
                .orElseThrow(() -> new IllegalStateException("Seed user 'user' not found"));

        Set<UUID> seededDocuments = new HashSet<>();

        try {
            UUID documentId = dataUtils.createDocumentWithStatus(owner, DocumentStatus.PROCESSED);
            seededDocuments.add(documentId);

            mockMvc.perform(get("/documents/{id}", documentId)
                            .header("Authorization", "Bearer " + ownerToken))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/documents/{id}", documentId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/documents/{id}", documentId)
                            .header("Authorization", "Bearer " + reviewerToken))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/documents/{id}", documentId)
                            .header("Authorization", "Bearer " + otherToken))
                    .andExpect(status().isForbidden());

            UUID docForDelete = dataUtils.createDocumentWithStatus(owner, DocumentStatus.PROCESSED);
            seededDocuments.add(docForDelete);

            mockMvc.perform(delete("/documents/{id}", docForDelete)
                            .header("Authorization", "Bearer " + otherToken))
                    .andExpect(status().isForbidden());

            mockMvc.perform(delete("/documents/{id}", docForDelete)
                            .header("Authorization", "Bearer " + ownerToken))
                    .andExpect(status().isOk());

            UUID docForAdminDelete = dataUtils.createDocumentWithStatus(owner, DocumentStatus.PROCESSED);
            seededDocuments.add(docForAdminDelete);

            mockMvc.perform(delete("/documents/{id}", docForAdminDelete)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());

        } finally {
            dataUtils.deleteDocumentsWithRelations(seededDocuments);
            dataUtils.deleteUserWithTokens(otherUser.getUsername());
        }
    }

    private void conversationAccessSpotChecks() throws Exception {
        String adminToken = auth.loginAndGetAccessToken("admin", "Admin#12345");
        String reviewerToken = auth.loginAndGetAccessToken("reviewer", "Reviewer#12345");

        TestUser otherReviewer = dataUtils.createIsolatedUser("OtherReviewer#12345");
        String otherReviewerToken = auth.loginAndGetAccessToken(otherReviewer.getUsername(), otherReviewer.getRawPassword());

        UserEntity owner = userRepository.findByUsername("user")
                .orElseThrow(() -> new IllegalStateException("Seed user 'user' not found"));
        UserEntity reviewer = userRepository.findByUsername("reviewer")
                .orElseThrow(() -> new IllegalStateException("Reviewer not found"));

        Set<UUID> seededDocuments = new HashSet<>();

        try {
            UUID documentId = dataUtils.createDocumentAssignedToReviewer(owner, reviewer, DocumentStatus.IN_REVIEW);
            seededDocuments.add(documentId);

            when(vectorQueryPort.searchByQuery(anyString(), anyDouble(), anyInt(), anyMap()))
                    .thenReturn(java.util.List.of());
            when(chatCompletionPort.askWithContext(anyString(), anyString(), any(UUID.class), anyList(), anyMap()))
                    .thenReturn("Test response");

            mockMvc.perform(post("/documents/{documentId}/conversations", documentId)
                            .param("question", "What is the status?")
                            .header("Authorization", "Bearer " + reviewerToken)
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/documents/{documentId}/conversations", documentId)
                            .param("question", "Can admin access?")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post("/documents/{documentId}/conversations", documentId)
                            .param("question", "Can other reviewer access?")
                            .header("Authorization", "Bearer " + otherReviewerToken)
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                    .andExpect(status().isForbidden());

            mockMvc.perform(delete("/documents/{documentId}/conversations", documentId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isForbidden());

            mockMvc.perform(delete("/documents/{documentId}/conversations", documentId)
                            .header("Authorization", "Bearer " + reviewerToken))
                    .andExpect(status().isOk());

        } finally {
            dataUtils.deleteDocumentsWithRelations(seededDocuments);
            dataUtils.deleteUserWithTokens(otherReviewer.getUsername());
        }
    }
}
