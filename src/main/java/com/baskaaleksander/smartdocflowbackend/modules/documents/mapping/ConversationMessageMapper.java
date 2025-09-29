package com.baskaaleksander.smartdocflowbackend.modules.documents.mapping;

import com.baskaaleksander.smartdocflowbackend.modules.documents.api.dto.ConversationMessageResponse;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.ConversationMessage;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ConversationMessageMapper {
    ConversationMessageResponse toMessageResponse(ConversationMessage message);
}
