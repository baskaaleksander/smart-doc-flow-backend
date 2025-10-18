package com.baskaaleksander.smartdocflowbackend.modules.reviews.application;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationUtil;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.api.dto.ReviewEventResponse;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewEventType;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.mapping.ReviewEventMapper;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.entity.ReviewEventEntity;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.spring.SpringDataReviewEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ReviewEventApplicationService {


    private final SpringDataReviewEventRepository reviewEventRepository;
    private final ReviewEventMapper reviewEventMapper;

    public ReviewEventApplicationService(SpringDataReviewEventRepository reviewEventRepository, ReviewEventMapper reviewEventMapper) {
        this.reviewEventRepository = reviewEventRepository;
        this.reviewEventMapper = reviewEventMapper;
    }

    public ReviewEventResponse getReviewEventById(UUID id) {
        return reviewEventMapper.toReviewEventResponse(
                reviewEventRepository.getReviewEventById(id).orElseThrow(() -> new ResourceNotFoundException("Review event with id " + id + " not found"))
        );
    }

    public PagingResult<ReviewEventResponse> getReviewEvents(UUID id, PaginationRequest request, String eventType) {
        Pageable pageable = PaginationUtil.getPageable(request);
        Page<ReviewEventEntity> reviewEvents;

        if (eventType.equalsIgnoreCase("ALL")) {
            reviewEvents = reviewEventRepository.findByReviewId(pageable, id);
        } else {
            reviewEvents = reviewEventRepository.findByReviewIdWithType(pageable, id, ReviewEventType.fromString(eventType));
        }

        List<ReviewEventResponse> reviewEventsList = reviewEvents
                .stream()
                .map(reviewEventMapper::toReviewEventResponse)
                .toList();

        Integer currentPage = request.getPage();
        int totalPages = reviewEvents.getTotalPages();

        return new PagingResult<>(
                reviewEventsList,
                totalPages,
                reviewEvents.getTotalElements(),
                reviewEvents.getSize(),
                reviewEvents.getNumber(),
                currentPage + 1 == totalPages,
                currentPage + 1 < totalPages
        );
    }
}
