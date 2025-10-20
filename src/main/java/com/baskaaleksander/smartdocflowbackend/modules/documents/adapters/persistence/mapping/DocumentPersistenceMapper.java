package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.mapping;

import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.entity.DocumentEntity;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.Document;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentReviewBasic;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentUserBasic;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.entity.ReviewEntity;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.entity.ReviewEventEntity;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface DocumentPersistenceMapper {

    @Mappings({
            @Mapping(target = "owner", source = "owner", qualifiedByName = "ownerToBasic"),
            @Mapping(target = "review", source = "review", qualifiedByName = "reviewToBasic")
    })
    Document toDomain(DocumentEntity entity);

    @Named("reviewToBasic")
    default DocumentReviewBasic reviewToBasic(ReviewEntity entity) {
        if (entity == null) return null;
        List<UUID> reviewEventIds = List.of();

        if (entity.getReviewEvents() != null && !entity.getReviewEvents().isEmpty()) {
            reviewEventIds = entity.getReviewEvents().stream().map(ReviewEventEntity::getId).toList();
        }


        return new DocumentReviewBasic(
                entity.getId(),
                entity.getReviewer() != null ? entity.getReviewer().getUsername() : null,
                entity.getReviewer() != null ? entity.getReviewer().getId() : null,
                entity.getDocument().getId(),
                !reviewEventIds.isEmpty() ? reviewEventIds : null,
                entity.getComment() != null ? entity.getComment() : null,
                entity.getStatus().getValue(),
                entity.getUpdatedAt()
        );
    }

    @Named("ownerToBasic")
    default DocumentUserBasic ownerToBasic(UserEntity entity) {
        return entity == null ? null :
                new DocumentUserBasic(
                        entity.getId(),
                        entity.getUsername()
                );
    }
}
