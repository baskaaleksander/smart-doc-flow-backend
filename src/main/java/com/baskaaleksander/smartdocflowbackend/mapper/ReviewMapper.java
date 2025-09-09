package com.baskaaleksander.smartdocflowbackend.mapper;

import com.baskaaleksander.smartdocflowbackend.dto.response.ReviewResponse;
import com.baskaaleksander.smartdocflowbackend.model.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(target = "documentId", source = "document.id")
    @Mapping(target = "reviewerId", source = "reviewer.id")
    ReviewResponse toReviewResponse(Review review);
}
