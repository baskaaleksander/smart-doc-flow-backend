package com.baskaaleksander.smartdocflowbackend.modules.document.application.conversation;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceConflictException;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.common.util.MakeConversationId;
import com.baskaaleksander.smartdocflowbackend.modules.documents.api.dto.ConversationMessageResponse;
import com.baskaaleksander.smartdocflowbackend.modules.documents.application.conversation.ConversationEncryptionService;
import com.baskaaleksander.smartdocflowbackend.modules.documents.application.conversation.ConversationService;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.ConversationSide;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.documents.mapping.ConversationMessageMapper;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.ConversationMessage;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.ConversationMessageRepository;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.Document;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class ConversationServiceTest {

    @Mock
    private VectorStore vectorStore;
    @Mock
    private DocumentRepository documentRepository;
    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
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
    private ConversationService realService;

    private ConversationMessage conversationMessage;
    private Document document;
    private UUID conversationId;
    private UUID documentId;
    private UUID ownerId;
    private ConversationService conversationService;



    @BeforeEach
    void setUp() {
        conversationService = spy(realService);
        documentId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        conversationId = UUID.fromString(MakeConversationId.makeConversationId(documentId.toString(), ownerId));
        conversationMessage = new ConversationMessage();
        conversationMessage.setId(UUID.randomUUID());
        conversationMessage.setConversationId(conversationId);
        conversationMessage.setUserId(ownerId);
        conversationMessage.setDocumentId(documentId);
        conversationMessage.setContent("example");
        conversationMessage.setSide(ConversationSide.USER);
        conversationMessage.setCreatedAt(Instant.now());
        document = new Document();
        document.setId(documentId);
        document.setStatus(DocumentStatus.PROCESSED);
    }

    @Test
    void askQuestion_shouldThrowException_whenDocumentNotFound() {
        when(documentRepository.getDocumentById(documentId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->conversationService.askQuestion("question", documentId, ownerId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void askQuestion_shouldThrowException_whenDocumentIsInWrongStatus() {
        document.setStatus(DocumentStatus.UPLOADED);

        when(documentRepository.getDocumentById(documentId))
                .thenReturn(Optional.of(document));

        assertThatThrownBy(() -> conversationService.askQuestion("question", documentId, ownerId))
                .isInstanceOf(ResourceConflictException.class);
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

    @Test
    void getAllConversationMessages_shouldReturnPagingResult() {
        PaginationRequest req = new PaginationRequest(0, 1, "createdAt", Sort.Direction.DESC);
        Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt"));

        when(conversationMessageRepository.findAllByDocumentIdAndUserId(
                any(Pageable.class), any(UUID.class), any(UUID.class)))
                .thenReturn(new PageImpl<>(List.of(conversationMessage), pageable, 1));

        when(conversationMessageMapper.toMessageResponse(any(ConversationMessage.class)))
                .thenAnswer(inv -> {
                    ConversationMessage msg = inv.getArgument(0);
                    return new ConversationMessageResponse(msg.getId(), msg.getSide(), "content", msg.getCreatedAt());
                });

        when(conversationEncryptionService.decrypt(any())).thenReturn("content");

        PagingResult<ConversationMessageResponse> res =
                conversationService.getAllConversationMessages(documentId, ownerId, req);

        assertThat(res).isNotNull();
        assertThat(res.content()).hasSize(1);
        assertThat(res.totalElements()).isEqualTo(1);
        assertThat(res.totalPages()).isEqualTo(1);
        assertThat(res.page()).isEqualTo(0);
        assertThat(res.size()).isEqualTo(1);

        verify(conversationMessageRepository).findAllByDocumentIdAndUserId(any(Pageable.class), eq(ownerId), eq(documentId));
        verify(conversationEncryptionService, atLeastOnce()).decrypt(any());
        verify(conversationMessageMapper, atLeastOnce()).toMessageResponse(any(ConversationMessage.class));
    }
}
