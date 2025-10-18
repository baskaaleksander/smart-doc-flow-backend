package com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.mapping;

import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.entity.ReviewEventEntity;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewEvent;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ReviewEventPersistenceMapper {

    public ReviewEvent toDomain(ReviewEventEntity e) {

        UUID reviewerId = (e.getReviewer() != null) ? e.getReviewer().getId() : null;

        return new ReviewEvent(
                e.getId(),
                e.getEventType(),
                e.getComment() != null ? e.getComment() : null,
                reviewerId,
                e.getReview().getId(),
                e.getCreatedAt()
        );
    }
}
