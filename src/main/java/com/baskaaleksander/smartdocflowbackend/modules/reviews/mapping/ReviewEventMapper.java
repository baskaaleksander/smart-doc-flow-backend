package com.baskaaleksander.smartdocflowbackend.modules.reviews.mapping;

import com.baskaaleksander.smartdocflowbackend.modules.reviews.api.dto.ReviewEventResponse;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.persistence.ReviewEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewEventMapper {

    @Mapping(target = "reviewerId", source = "reviewer.id")
    ReviewEventResponse toReviewEventResponse(ReviewEvent reviewEvent);
}
