package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.mapping;

import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.entity.DocumentOcrResultEntity;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentOcrResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DocumentOcrResultPersistenceMapper {

    @Mapping(target = "documentId", source = "document.id")
    DocumentOcrResult toDomain(DocumentOcrResultEntity entity);
}
