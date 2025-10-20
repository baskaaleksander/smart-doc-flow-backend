package com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port;

import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.ConversationMessage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationMessageQueryPort {
    List<UUID> getUserIdByDocumentId(UUID documentId);
    Optional<UUID> getIdByUserIdAndDocId(UUID userId, UUID documentId);
    PagingResult<ConversationMessage> findAllByDocumentIdAndUserId(PaginationRequest request, UUID userId, UUID documentId);
}
