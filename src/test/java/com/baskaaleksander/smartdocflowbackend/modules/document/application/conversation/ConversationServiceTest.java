package com.baskaaleksander.smartdocflowbackend.modules.document.application.conversation;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.util.MakeConversationId;
import com.baskaaleksander.smartdocflowbackend.modules.documents.api.dto.ConversationMessageResponse;
import com.baskaaleksander.smartdocflowbackend.modules.documents.application.conversation.ConversationEncryptionService;
import com.baskaaleksander.smartdocflowbackend.modules.documents.application.conversation.ConversationService;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.ConversationSide;
import com.baskaaleksander.smartdocflowbackend.modules.documents.mapping.ConversationMessageMapper;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.ConversationMessage;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.ConversationMessageRepository;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class ConversationServiceTest {

    @Mock
    private VectorStore vectorStore;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private ChatClient chatClient;
    @Mock
    private ConversationEncryptionService conversationEncryptionService;
    @Mock
    private ConversationMessageRepository conversationMessageRepository;
    @Mock
    private JdbcChatMemoryRepository jdbcChatMemoryRepository;
    @Mock
    private ConversationMessageMapper conversationMessageMapper;

    @InjectMocks
    private ConversationService conversationService;

    private ConversationMessage conversationMessage;
    private UUID conversationId;
    private UUID documentId;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        documentId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        conversationId = UUID.fromString(MakeConversationId.makeConversationId(documentId.toString(), ownerId));
        conversationMessage = new ConversationMessage();
        conversationMessage.setConversationId(conversationId);
        conversationMessage.setUserId(ownerId);
        conversationMessage.setDocumentId(documentId);
        conversationMessage.setContent("example");
        conversationMessage.setSide(ConversationSide.USER);
    }

    @Test
    void deleteConversation_shouldEndWithSuccess() {
        when(conversationMessageRepository.getIdByUserIdAndDocId(documentId, ownerId)).thenReturn(Optional.of(conversationId));

        conversationService.deleteConversation(documentId, ownerId);

        verify(jdbcChatMemoryRepository).deleteByConversationId(conversationId.toString());
        verify(conversationMessageRepository).deleteAllByConversationId(conversationId);
    }

    @Test
    void deleteConversation_shouldThrowException_whenConvoIdNotFound() {
        when(conversationMessageRepository.getIdByUserIdAndDocId(documentId, ownerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversationService.deleteConversation(documentId, ownerId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
