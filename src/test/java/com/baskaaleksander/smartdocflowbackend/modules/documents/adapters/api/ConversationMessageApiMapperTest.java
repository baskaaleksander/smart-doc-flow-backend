package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api;

import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api.dto.ConversationMessageResponse;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.ConversationMessage;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.ConversationSide;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationMessageApiMapperTest {

    private final ConversationMessageApiMapper mapper = Mappers.getMapper(ConversationMessageApiMapper.class);

    @Test
    void toResponse_mapsAllFieldsCorrectly() {
        UUID id = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        ConversationMessage message = new ConversationMessage();
        message.setId(id);
        message.setConversationId(conversationId);
        message.setUserId(userId);
        message.setDocumentId(documentId);
        message.setSide(ConversationSide.USER);
        message.setContent("encryptedText");
        message.setFingerprint("hash123");
        message.setCreatedAt(Instant.now());

        ConversationMessageResponse response = mapper.toResponse(message);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getSide()).isEqualTo(ConversationSide.USER);
        assertThat(response.getContent()).isEqualTo("encryptedText");
        assertThat(response.getCreatedAt()).isEqualTo(message.getCreatedAt());
    }

    @Test
    void toResponse_returnsNull_whenInputIsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}