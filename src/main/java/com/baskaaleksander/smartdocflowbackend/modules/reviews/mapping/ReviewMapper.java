package com.baskaaleksander.smartdocflowbackend.modules.reviews.mapping;

import com.baskaaleksander.smartdocflowbackend.modules.reviews.api.dto.ReviewResponse;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.persistence.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(target = "documentId", source = "document.id")
    @Mapping(target = "reviewerId", source = "reviewer.id")
    ReviewResponse toReviewResponse(Review review);
}
