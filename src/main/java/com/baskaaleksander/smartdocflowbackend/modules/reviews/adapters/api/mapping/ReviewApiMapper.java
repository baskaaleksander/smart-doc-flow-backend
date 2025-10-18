package com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.api.mapping;

import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.api.dto.ReviewResponse;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.Review;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReviewApiMapper {

    ReviewResponse toReviewResponse(Review review);
}
