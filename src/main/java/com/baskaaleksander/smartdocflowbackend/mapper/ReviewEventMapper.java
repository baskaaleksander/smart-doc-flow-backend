package com.baskaaleksander.smartdocflowbackend.mapper;

import com.baskaaleksander.smartdocflowbackend.dto.response.ReviewEventResponse;
import com.baskaaleksander.smartdocflowbackend.model.ReviewEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewEventMapper {

    @Mapping(target = "reviewerId", source = "reviewer.id")
    ReviewEventResponse toReviewEventResponse(ReviewEvent reviewEvent);
}
