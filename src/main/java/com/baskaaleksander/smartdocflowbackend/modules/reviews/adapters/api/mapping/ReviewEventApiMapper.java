package com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.api.mapping;

import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.api.dto.ReviewEventResponse;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewEvent;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReviewEventApiMapper {

    ReviewEventResponse toEventResponse(ReviewEvent event);
}
