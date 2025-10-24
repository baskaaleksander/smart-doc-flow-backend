package com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.api.mapping;

import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.api.dto.ReviewEventResponse;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewEvent;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewEventType;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewerBasic;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewEventApiMapperTest {

    private final ReviewEventApiMapper mapper = Mappers.getMapper(ReviewEventApiMapper.class);

    @Test
    void toEventResponse_mapsAllFieldsCorrectly() {
        UUID eventId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();
        ReviewerBasic reviewer = new ReviewerBasic(reviewerId, "john");
        Instant createdAt = Instant.now();

        ReviewEvent event = new ReviewEvent();
        event.setId(eventId);
        event.setReviewId(reviewId);
        event.setReviewer(reviewer);
        event.setEventType(ReviewEventType.COMMENT);
        event.setComment("Looks good");
        event.setCreatedAt(createdAt);

        ReviewEventResponse dto = mapper.toEventResponse(event);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(eventId);
        assertThat(dto.reviewId()).isEqualTo(reviewId);
        assertThat(dto.reviewer().name()).isEqualTo("john");
        assertThat(dto.reviewer().id()).isEqualTo(reviewerId);
        assertThat(dto.eventType()).isEqualTo(ReviewEventType.COMMENT);
        assertThat(dto.comment()).isEqualTo("Looks good");
        assertThat(dto.createdAt()).isEqualTo(createdAt);
    }
}