package com.baskaaleksander.smartdocflowbackend.modules.reviews.application;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.api.dto.ReviewEventResponse;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.api.mapping.ReviewEventApiMapper;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewEvent;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewEventType;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.port.ReviewEventQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewEventApplicationService {

    private final ReviewEventQueryPort reviewEventQueryPort;
    private final ReviewEventApiMapper mapper;

    public ReviewEventResponse getReviewEventById(UUID id) {
        return mapper.toEventResponse(
                reviewEventQueryPort.getReviewEventById(id).orElseThrow(() -> new ResourceNotFoundException("Review event not found"))
        );
    }

    public PagingResult<ReviewEventResponse> getReviewEvents(UUID id, PaginationRequest request, String eventType) {
        PagingResult<ReviewEvent> reviewEvents;

        if (eventType.equalsIgnoreCase("ALL")) {
            reviewEvents = reviewEventQueryPort.findByReviewId(request, id);
        } else {
            reviewEvents = reviewEventQueryPort.findByReviewIdWithType(request, id, ReviewEventType.fromString(eventType));
        }

        List<ReviewEventResponse> reviewEventsList = reviewEvents.content()
                .stream()
                .map(mapper::toEventResponse)
                .toList();

        return new PagingResult<>(
                reviewEventsList,
                reviewEvents.totalPages(),
                reviewEvents.totalElements(),
                reviewEvents.size(),
                reviewEvents.page(),
                reviewEvents.last(),
                reviewEvents.next()
        );
    }
}
