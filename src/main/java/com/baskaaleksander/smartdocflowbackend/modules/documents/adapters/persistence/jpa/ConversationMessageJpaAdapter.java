package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.jpa;

import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.ConversationMessage;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.ConversationMessageCommandPort;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.ConversationMessageQueryPort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class ConversationMessageJpaAdapter implements ConversationMessageCommandPort, ConversationMessageQueryPort {
    @Override
    @Transactional
    public ConversationMessage save(ConversationMessage message) {
        return null;
    }

    @Override
    @Transactional
    public void deleteAllByConversationId(UUID conversationId) {

    }

    @Override
    public List<UUID> getUserIdByDocumentId(UUID documentId) {
        return List.of();
    }

    @Override
    public Optional<UUID> getIdByUserIdAndDocId(UUID userId, UUID documentId) {
        return Optional.empty();
    }

    @Override
    public PagingResult<ConversationMessage> findAllByDocumentIdAndUserId(PaginationRequest request, UUID userId, UUID documentId) {
        return null;
    }
}
