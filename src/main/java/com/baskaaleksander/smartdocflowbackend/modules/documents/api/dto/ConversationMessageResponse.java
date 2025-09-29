package com.baskaaleksander.smartdocflowbackend.modules.documents.api.dto;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.ConversationSide;

import java.util.UUID;

public record ConversationMessageResponse(
    UUID id,
    ConversationSide side,
    String content
) {
}
