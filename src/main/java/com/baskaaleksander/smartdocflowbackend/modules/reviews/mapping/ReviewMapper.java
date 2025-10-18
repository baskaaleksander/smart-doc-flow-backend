package com.baskaaleksander.smartdocflowbackend.modules.reviews.mapping;

import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.api.dto.ReviewResponse;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.entity.ReviewEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(target = "documentId", source = "document.id")
    @Mapping(target = "reviewerId", source = "reviewer.id")
    ReviewResponse toReviewResponse(ReviewEntity review);
}
