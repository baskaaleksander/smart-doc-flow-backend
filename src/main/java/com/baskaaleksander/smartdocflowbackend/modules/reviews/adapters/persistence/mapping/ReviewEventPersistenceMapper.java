package com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.mapping;

import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.entity.ReviewEventEntity;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewEvent;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewerBasic;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ReviewEventPersistenceMapper {

    public ReviewEvent toDomain(ReviewEventEntity e) {

        ReviewerBasic reviewer = (e.getReviewer() != null) ? new ReviewerBasic(e.getReview().getId(), e.getReviewer().getUsername()) : null;

        return new ReviewEvent(
                e.getId(),
                e.getEventType(),
                e.getComment() != null ? e.getComment() : null,
                reviewer,
                e.getReview().getId(),
                e.getCreatedAt()
        );
    }
}
