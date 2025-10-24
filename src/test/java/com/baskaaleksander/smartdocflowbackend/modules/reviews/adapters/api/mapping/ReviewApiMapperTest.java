package com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.api.mapping;

import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.api.dto.ReviewResponse;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.Review;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewStatus;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewApiMapperTest {

    private final ReviewApiMapper mapper = Mappers.getMapper(ReviewApiMapper.class);

    @Test
    void toReviewResponse_mapsAllFieldsCorrectly() {
        UUID id = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();

        Review review = new Review();
        review.setId(id);
        review.setDocumentId(docId);
        review.setReviewerId(reviewerId);
        review.setStatus(ReviewStatus.PENDING);
        review.setComment("Needs review");

        ReviewResponse dto = mapper.toReviewResponse(review);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.documentId()).isEqualTo(docId);
        assertThat(dto.reviewerId()).isEqualTo(reviewerId);
        assertThat(dto.status()).isEqualTo(ReviewStatus.PENDING);
        assertThat(dto.comment()).isEqualTo("Needs review");
    }
}