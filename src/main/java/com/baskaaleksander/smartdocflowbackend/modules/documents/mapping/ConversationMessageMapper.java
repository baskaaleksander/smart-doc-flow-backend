package com.baskaaleksander.smartdocflowbackend.modules.documents.mapping;

import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api.dto.ConversationMessageResponse;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.entity.ConversationMessageEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ConversationMessageMapper {
    ConversationMessageResponse toMessageResponse(ConversationMessageEntity message);
}
