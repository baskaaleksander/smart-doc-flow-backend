package com.baskaaleksander.smartdocflowbackend.modules.reviews.mapping;

import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.api.dto.EventReviewerBasicInfo;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.api.dto.ReviewEventResponse;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.entity.ReviewEventEntity;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ReviewEventMapper {

    @Mapping(target = "reviewer", source = "reviewer", qualifiedByName = "toReviewerBasicInfo")
    @Mapping(target= "reviewId", source = "review.id")
    ReviewEventResponse toReviewEventResponse(ReviewEventEntity reviewEvent);

    @Named("toReviewerBasicInfo")
    default EventReviewerBasicInfo toReviewerBasicInfo(UserEntity reviewer) {
        return reviewer == null ? null :
                new EventReviewerBasicInfo(
                        reviewer.getId(),
                        reviewer.getUsername()
                );
    }
}
