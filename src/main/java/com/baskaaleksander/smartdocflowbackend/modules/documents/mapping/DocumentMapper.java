package com.baskaaleksander.smartdocflowbackend.modules.documents.mapping;

import com.baskaaleksander.smartdocflowbackend.modules.documents.api.dto.DocumentOwnerBasicInfo;
import com.baskaaleksander.smartdocflowbackend.modules.documents.api.dto.DocumentResponse;
import com.baskaaleksander.smartdocflowbackend.modules.documents.api.dto.DocumentReviewBasicInfo;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.Document;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.persistence.Review;
import com.baskaaleksander.smartdocflowbackend.modules.users.persistence.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    @Mappings({
            @Mapping(target = "owner", source = "owner", qualifiedByName = "ownerToBasicInfo"),
            @Mapping(target = "review", source = "review", qualifiedByName = "reviewToBasicInfo"),
    })
    DocumentResponse toDocumentResponse(Document document);

    @Named("reviewToBasicInfo")
    default DocumentReviewBasicInfo reviewToBasicInfo(Review review) {
        if (review == null) return null;

        User reviewer = review.getReviewer();
        UUID reviewerId = reviewer != null ? reviewer.getId() : null;
        String reviewerUsername = reviewer != null ? reviewer.getUsername() : null;

        return new DocumentReviewBasicInfo(
                review.getId(),
                reviewerUsername,
                reviewerId,
                review.getStatus(),
                review.getUpdatedAt()
        );
    }

    @Named("ownerToBasicInfo")
    default DocumentOwnerBasicInfo ownerToBasicInfo(User owner) {
        return owner == null ? null :
                new DocumentOwnerBasicInfo(
                        owner.getId(),
                        owner.getUsername()
                );
    }
}
