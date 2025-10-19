package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api.dto;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.ConversationSide;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor @NoArgsConstructor
@Getter @Setter
public class ConversationMessageResponse {
    private UUID id;
    private ConversationSide side;
    private String content;
    private Instant createdAt;
}
