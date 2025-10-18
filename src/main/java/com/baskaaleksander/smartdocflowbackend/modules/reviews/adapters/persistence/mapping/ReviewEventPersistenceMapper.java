package com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.mapping;

import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.entity.ReviewEventEntity;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewEvent;
import org.springframework.stereotype.Component;

@Component
public class ReviewEventPersistenceMapper {

    public ReviewEvent toDomain(ReviewEventEntity e) {
        return new ReviewEvent(
                e.getId(),
                e.getEventType(),
                e.getComment(),
                e.getReviewer().getId(),
                e.getReview().getId(),
                e.getCreatedAt()
        );
    }
}
