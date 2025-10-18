package com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.mapping;

import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.entity.ReviewEntity;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.Review;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class ReviewPersistenceMapper {

    private final ReviewEventPersistenceMapper eventMapper;

    public ReviewPersistenceMapper(ReviewEventPersistenceMapper eventMapper) {
        this.eventMapper = eventMapper;
    }

    Review toDomain(ReviewEntity e) {
        return new Review(
                e.getId(),
                e.getDocument().getId(),
                e.getStatus(),
                e.getReviewer().getId(),
                e.getReviewEvents().stream().map(eventMapper::toDomain).toList(),
                e.getComment(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getVersion()
        );
    }
}
