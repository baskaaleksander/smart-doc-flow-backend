package com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.mapping;

import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.entity.ReviewEntity;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.Review;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ReviewPersistenceMapper {

    private final ReviewEventPersistenceMapper eventMapper;

    public ReviewPersistenceMapper(ReviewEventPersistenceMapper eventMapper) {
        this.eventMapper = eventMapper;
    }


    public Review toDomain(ReviewEntity e) {

        UUID reviewerId = (e.getReviewer() != null) ? e.getReviewer().getId() : null;

        return new Review(
                e.getId(),
                e.getDocument().getId(),
                e.getStatus(),
                reviewerId,
                e.getReviewEvents().stream().map(eventMapper::toDomain).toList(),
                e.getComment(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getVersion()
        );
    }
}
