package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api;

import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api.dto.ConversationMessageResponse;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.ConversationMessage;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ConversationMessageApiMapper {

    ConversationMessageResponse toResponse(ConversationMessage message);
}
