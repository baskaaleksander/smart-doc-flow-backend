package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api;

import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api.dto.DocumentResponse;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.Document;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DocumentApiMapper {
    DocumentResponse toResponse(Document document);
}
