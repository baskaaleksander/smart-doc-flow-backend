package com.baskaaleksander.smartdocflowbackend.modules.documents.application.conversation;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceConflictException;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.logging.LoggingPort;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.common.util.MakeConversationId;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api.ConversationMessageApiMapper;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api.dto.ConversationMessageResponse;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.*;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationEncryptionServicePort encryption;
    @Mock
    private DocumentQueryPort documentQueryPort;
    @Mock
    private ConversationMessageQueryPort conversationMessageQueryPort;
    @Mock
    private ConversationMessageCommandPort conversationMessageCommandPort;
    @Mock
    private ConversationMessageApiMapper mapper;
    @Mock
    private VectorQueryPort vectorQueryPort;
    @Mock
    private ChatCompletionPort chatCompletionPort;
    @Mock
    private LoggingPort logger;

    @InjectMocks
    private ConversationService service;

    @Test
    void askQuestion_success() {
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Document doc = new Document.Builder()
                .id(docId)
                .filename("file.pdf")
                .mime("application/pdf")
                .size(2.0)
                .storageKey(docId + "_file.pdf")
                .pageSize(0)
                .status(DocumentStatus.PROCESSED)
                .build();
        when(documentQueryPort.getDocumentById(docId)).thenReturn(Optional.of(doc));

        SearchHit h1 = mock(SearchHit.class);
        SearchHit h2 = mock(SearchHit.class);
        when(h1.text()).thenReturn("c1");
        when(h2.text()).thenReturn("c2");
        when(vectorQueryPort.searchByQuery(eq("What is inside?"), anyDouble(), anyInt(), anyMap()))
                .thenReturn(List.of(h1, h2));

        when(chatCompletionPort.askWithContext(eq("What is inside?"), anyString(), eq(docId), eq(List.of("c1", "c2")), anyMap()))
                .thenReturn("Answer");

        when(encryption.encrypt("What is inside?")).thenReturn("enc_q");
        when(encryption.fingerprint("What is inside?")).thenReturn("fp_q");
        when(encryption.encrypt("Answer")).thenReturn("enc_a");
        when(encryption.fingerprint("Answer")).thenReturn("fp_a");

        String out = service.askQuestion("What is inside?", docId, userId);

        assertThat(out).isEqualTo("Answer");

        String expectedConversationId = MakeConversationId.makeConversationId(docId.toString(), userId);

        ArgumentCaptor<ConversationMessage> captor = ArgumentCaptor.forClass(ConversationMessage.class);
        verify(conversationMessageCommandPort, times(2)).save(captor.capture());
        List<ConversationMessage> saved = captor.getAllValues();

        ConversationMessage first = saved.get(0);
        assertThat(first.getSide()).isEqualTo(ConversationSide.USER);
        assertThat(first.getContent()).isEqualTo("enc_q");
        assertThat(first.getFingerprint()).isEqualTo("fp_q");
        assertThat(first.getConversationId().toString()).isEqualTo(expectedConversationId);
        assertThat(first.getUserId()).isEqualTo(userId);
        assertThat(first.getDocumentId()).isEqualTo(docId);

        ConversationMessage second = saved.get(1);
        assertThat(second.getSide()).isEqualTo(ConversationSide.SYSTEM);
        assertThat(second.getContent()).isEqualTo("enc_a");
        assertThat(second.getFingerprint()).isEqualTo("fp_a");
        assertThat(second.getConversationId().toString()).isEqualTo(expectedConversationId);
    }

    @Test
    void askQuestion_docNotProcessed_conflict() {
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Document doc = new Document.Builder()
                .id(docId)
                .filename("file.pdf")
                .mime("application/pdf")
                .size(2.0)
                .storageKey(docId + "_file.pdf")
                .pageSize(0)
                .status(DocumentStatus.UPLOADED)
                .build();
        when(documentQueryPort.getDocumentById(docId)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> service.askQuestion("Q", docId, userId))
                .isInstanceOf(ResourceConflictException.class);

        verifyNoInteractions(vectorQueryPort, chatCompletionPort);
    }

    @Test
    void askQuestion_docNotFound() {
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(documentQueryPort.getDocumentById(docId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.askQuestion("Q", docId, userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteConversation_success() {
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        when(conversationMessageQueryPort.getConversationIdByUserIdAndDocId(userId, docId)).thenReturn(Optional.of(conversationId));

        service.deleteConversation(docId, userId);

        verify(conversationMessageCommandPort).deleteAllByConversationId(conversationId);
    }

    @Test
    void deleteConversation_notFound() {
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(conversationMessageQueryPort.getConversationIdByUserIdAndDocId(userId, docId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteConversation(docId, userId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(conversationMessageCommandPort, never()).deleteAllByConversationId(any());
    }

    @Test
    void getAllConversationMessages_decryptsAndMaps() {
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PaginationRequest req = new PaginationRequest(0, 2, null, null);

        ConversationMessage m1 = new ConversationMessage();
        m1.setId(UUID.randomUUID());
        m1.setContent("enc1");
        m1.setSide(ConversationSide.USER);
        ConversationMessage m2 = new ConversationMessage();
        m2.setId(UUID.randomUUID());
        m2.setContent("enc2");
        m2.setSide(ConversationSide.SYSTEM);

        PagingResult<ConversationMessage> page = new PagingResult<>(List.of(m1, m2), 1, 2L, 2, 0, true, false);
        when(conversationMessageQueryPort.findAllByDocumentIdAndUserId(req, userId, docId)).thenReturn(page);

        ConversationMessageResponse r1 = new ConversationMessageResponse();
        r1.setId(m1.getId());
        r1.setContent("enc1");
        r1.setSide(ConversationSide.USER);
        ConversationMessageResponse r2 = new ConversationMessageResponse();
        r2.setId(m2.getId());
        r2.setContent("enc2");
        r2.setSide(ConversationSide.SYSTEM);

        when(mapper.toResponse(m1)).thenReturn(r1);
        when(mapper.toResponse(m2)).thenReturn(r2);

        when(encryption.decrypt("enc1")).thenReturn("plain1");
        when(encryption.decrypt("enc2")).thenReturn("plain2");

        PagingResult<ConversationMessageResponse> out = service.getAllConversationMessages(docId, userId, req);

        assertThat(out.totalPages()).isEqualTo(1);
        assertThat(out.totalElements()).isEqualTo(2L);
        assertThat(out.content()).extracting(ConversationMessageResponse::getContent)
                .containsExactly("plain1", "plain2");
    }
}
