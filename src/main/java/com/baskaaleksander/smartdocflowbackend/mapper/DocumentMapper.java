package com.baskaaleksander.smartdocflowbackend.mapper;

import com.baskaaleksander.smartdocflowbackend.dto.response.DocumentResponse;
import com.baskaaleksander.smartdocflowbackend.dto.response.UserResponse;
import com.baskaaleksander.smartdocflowbackend.model.Document;
import com.baskaaleksander.smartdocflowbackend.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    DocumentMapper INSTANCE = Mappers.getMapper(DocumentMapper.class);

    DocumentResponse toDocumentResponse(Document document);
}
