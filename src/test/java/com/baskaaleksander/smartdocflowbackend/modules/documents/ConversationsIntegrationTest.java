package com.baskaaleksander.smartdocflowbackend.modules.documents;

import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.entity.ConversationMessageEntity;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.spring.SpringDataConversationMessageRepository;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.ConversationSide;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.SearchHit;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.ChatCompletionPort;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.ConversationEncryptionServicePort;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.VectorQueryPort;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.AuthTestUtils;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.IntegrationTestBase;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.TestDataSeeder;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.TestDataUtils;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.entity.UserEntity;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.spring.SpringDataUserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConversationsIntegrationTest extends IntegrationTestBase {

    @TestConfiguration
    static class OverrideAiPorts {

        @Bean
        @Primary
        ChatCompletionPort chatCompletionPort() {
            return Mockito.mock(ChatCompletionPort.class);
        }

        @Bean
        @Primary
        VectorQueryPort vectorQueryPort() {
            return Mockito.mock(VectorQueryPort.class);
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
    private SpringDataConversationMessageRepository conversationRepository;

    @Autowired
    private ConversationEncryptionServicePort encryptionService;

    @Autowired
    private ChatCompletionPort chatCompletionPort;

    @Autowired
    private VectorQueryPort vectorQueryPort;

    @BeforeAll
    void init() {
        seeder.seedAccountsIfNotExists();
    }

    @Test
    void create_successAssignedReviewer() throws Exception {
        String reviewerToken = auth.loginAndGetAccessToken("reviewer", "Reviewer#12345");

        UserEntity owner = userRepository.findByUsername("user")
                .orElseThrow(() -> new IllegalStateException("Seed user 'user' not found"));
        UserEntity reviewer = userRepository.findByUsername("reviewer")
                .orElseThrow(() -> new IllegalStateException("Reviewer not found"));

        UUID documentId = dataUtils.createDocumentAssignedToReviewer(owner, reviewer, DocumentStatus.IN_REVIEW);

        when(vectorQueryPort.searchByQuery(anyString(), anyDouble(), anyInt(), anyMap()))
                .thenReturn(List.of(
                        new SearchHit("The policy requires a signature.", 0.92, Map.of()),
                        new SearchHit("Ensure the contract is reviewed by legal.", 0.88, Map.of())
                ));

        when(chatCompletionPort.askWithContext(anyString(), anyString(), any(UUID.class), anyList(), anyMap()))
                .thenReturn("Answer: Please proceed with the legal review and signature collection.");

        MvcResult result = mockMvc.perform(post("/documents/{documentId}/conversations", documentId)
                        .param("question", "What is the next step?")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string("Answer: Please proceed with the legal review and signature collection."))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).isEqualTo("Answer: Please proceed with the legal review and signature collection.");

        List<ConversationMessageEntity> messages = conversationRepository.findAll().stream()
                .filter(msg -> msg.getDocumentId().equals(documentId) && msg.getUserId().equals(reviewer.getId()))
                .collect(Collectors.toList());

        assertThat(messages).hasSize(2);
        assertThat(messages.stream().map(ConversationMessageEntity::getSide))
                .containsExactlyInAnyOrder(ConversationSide.USER, ConversationSide.SYSTEM);

        Map<ConversationSide, String> decryptedBySide = messages.stream()
                .collect(Collectors.toMap(
                        ConversationMessageEntity::getSide,
                        msg -> encryptionService.decrypt(msg.getContent())
                ));

        assertThat(decryptedBySide.get(ConversationSide.USER)).isEqualTo("What is the next step?");
        assertThat(decryptedBySide.get(ConversationSide.SYSTEM)).isEqualTo("Answer: Please proceed with the legal review and signature collection.");

        mockMvc.perform(get("/documents/{documentId}/conversations", documentId)
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("What is the next step?")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Answer: Please proceed with the legal review and signature collection.")));

        conversationRepository.deleteAll(messages);
        dataUtils.deleteDocumentsWithRelations(Set.of(documentId));
    }
}
