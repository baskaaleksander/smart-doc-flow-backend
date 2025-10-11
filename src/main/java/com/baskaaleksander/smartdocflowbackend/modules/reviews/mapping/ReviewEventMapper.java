package com.baskaaleksander.smartdocflowbackend.modules.reviews.mapping;

import com.baskaaleksander.smartdocflowbackend.modules.reviews.api.dto.EventReviewerBasicInfo;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.api.dto.ReviewEventResponse;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.persistence.ReviewEvent;
import com.baskaaleksander.smartdocflowbackend.modules.users.persistence.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ReviewEventMapper {

    @Mapping(target = "reviewer", source = "reviewer", qualifiedByName = "toReviewerBasicInfo")
    @Mapping(target= "reviewId", source = "review.id")
    ReviewEventResponse toReviewEventResponse(ReviewEvent reviewEvent);

    @Named("toReviewerBasicInfo")
    default EventReviewerBasicInfo toReviewerBasicInfo(User reviewer) {
        return reviewer == null ? null :
                new EventReviewerBasicInfo(
                        reviewer.getId(),
                        reviewer.getUsername()
                );
    }
}
