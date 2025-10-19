package com.baskaaleksander.smartdocflowbackend.modules.documents.mapping;

import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api.dto.DocumentOwnerBasicResponse;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api.dto.DocumentResponse;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api.dto.DocumentReviewBasicResponse;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.entity.DocumentEntity;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.entity.ReviewEntity;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    @Mappings({
            @Mapping(target = "owner", source = "owner", qualifiedByName = "ownerToBasicInfo"),
            @Mapping(target = "review", source = "review", qualifiedByName = "reviewToBasicInfo"),
    })
    DocumentResponse toDocumentResponse(DocumentEntity document);

    @Named("reviewToBasicInfo")
    default DocumentReviewBasicResponse reviewToBasicInfo(ReviewEntity review) {
        if (review == null) return null;

        UserEntity reviewer = review.getReviewer();
        UUID reviewerId = reviewer != null ? reviewer.getId() : null;
        String reviewerUsername = reviewer != null ? reviewer.getUsername() : null;

        return new DocumentReviewBasicResponse(
                review.getId(),
                reviewerUsername,
                reviewerId,
                review.getStatus(),
                review.getUpdatedAt()
        );
    }

    @Named("ownerToBasicInfo")
    default DocumentOwnerBasicResponse ownerToBasicInfo(UserEntity owner) {
        return owner == null ? null :
                new DocumentOwnerBasicResponse(
                        owner.getId(),
                        owner.getUsername()
                );
    }
}
