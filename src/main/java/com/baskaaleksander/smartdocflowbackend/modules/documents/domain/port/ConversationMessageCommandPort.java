package com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.ConversationMessage;

import java.util.UUID;

public interface ConversationMessageCommandPort {
    ConversationMessage save(ConversationMessage message);
    void deleteAllByConversationId(UUID conversationId);
}
