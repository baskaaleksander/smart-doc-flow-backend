package com.baskaaleksander.smartdocflowbackend.modules.documents.mapping;

import com.baskaaleksander.smartdocflowbackend.modules.documents.api.dto.DocumentResponse;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.Document;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.persistence.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    DocumentMapper INSTANCE = Mappers.getMapper(DocumentMapper.class);

    @Mappings({
            @Mapping(target = "ownerId", source = "owner.id"),
            @Mapping(target = "reviewId", source = "review", qualifiedByName = "reviewToId"),
    })
    DocumentResponse toDocumentResponse(Document document);

    @Named("reviewToId")
    default UUID reviewToId(Review review) {
        return review == null ? null : review.getId();
    }
}
