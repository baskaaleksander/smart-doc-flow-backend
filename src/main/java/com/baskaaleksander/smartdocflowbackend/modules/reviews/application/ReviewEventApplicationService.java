package com.baskaaleksander.smartdocflowbackend.modules.reviews.application;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.logging.LoggingPort;
import com.baskaaleksander.smartdocflowbackend.common.logging.Slf4jLoggingAdapter;
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
    private final LoggingPort logger;

    public ReviewEventResponse getReviewEventById(UUID id) {
        logger.info("REVIEW_EVENT_GET START id=" + Slf4jLoggingAdapter.shortId(id));
        ReviewEvent event = reviewEventQueryPort.getReviewEventById(id)
                .orElseThrow(() -> {
                    logger.warn("REVIEW_EVENT_GET FAILED reason=not_found id=" + Slf4jLoggingAdapter.shortId(id));
                    return new ResourceNotFoundException("Review event not found");
                });
        logger.info("REVIEW_EVENT_GET SUCCESS id=" + Slf4jLoggingAdapter.shortId(id)
                + " type=" + event.getEventType());
        return mapper.toEventResponse(event);
    }

    public PagingResult<ReviewEventResponse> getReviewEvents(UUID id, PaginationRequest request, String eventType) {
        logger.info("REVIEW_EVENT_LIST START reviewId=" + Slf4jLoggingAdapter.shortId(id)
                + " page=" + request.getPage() + " size=" + request.getSize()
                + " type=" + (eventType == null ? "null" : eventType));

        PagingResult<ReviewEvent> reviewEvents;
        if (eventType != null && eventType.equalsIgnoreCase("ALL")) {
            reviewEvents = reviewEventQueryPort.findByReviewId(request, id);
        } else {
            ReviewEventType type = ReviewEventType.fromString(eventType);
            reviewEvents = reviewEventQueryPort.findByReviewIdWithType(request, id, type);
        }

        List<ReviewEventResponse> reviewEventsList = reviewEvents.content()
                .stream()
                .map(mapper::toEventResponse)
                .toList();

        logger.info("REVIEW_EVENT_LIST SUCCESS reviewId=" + Slf4jLoggingAdapter.shortId(id)
                + " returned=" + reviewEventsList.size()
                + " totalElements=" + reviewEvents.totalElements()
                + " totalPages=" + reviewEvents.totalPages());

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