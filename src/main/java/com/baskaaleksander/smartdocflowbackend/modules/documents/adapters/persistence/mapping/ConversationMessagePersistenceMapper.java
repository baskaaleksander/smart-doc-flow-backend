package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.mapping;

import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.entity.ConversationMessageEntity;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.ConversationMessage;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ConversationMessagePersistenceMapper {

    ConversationMessage toDomain(ConversationMessageEntity entity);
}
