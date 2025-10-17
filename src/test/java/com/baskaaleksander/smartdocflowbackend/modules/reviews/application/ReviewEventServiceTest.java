package com.baskaaleksander.smartdocflowbackend.modules.reviews.application;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.api.dto.EventReviewerBasicInfo;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.api.dto.ReviewEventResponse;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.ReviewEventType;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.mapping.ReviewEventMapper;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.persistence.Review;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.persistence.ReviewEvent;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.persistence.ReviewEventRepository;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReviewEventServiceTest {

    @Mock
    private ReviewEventRepository reviewEventRepository;

    @Mock
    private ReviewEventMapper reviewEventMapper;

    @InjectMocks
    private ReviewEventService reviewEventService;

    private ReviewEvent reviewEvent;

    @BeforeEach
    void setUp() {
        reviewEvent = new ReviewEvent();
        UserEntity reviewer = new UserEntity();
        Review review = new Review();

        reviewer.setId(UUID.randomUUID());
        review.setId(UUID.randomUUID());

        reviewEvent.setId(UUID.randomUUID());
        reviewEvent.setEventType(ReviewEventType.ASSIGNED);
        reviewEvent.setReviewer(reviewer);
        reviewEvent.setReview(review);
        reviewEvent.setCreatedAt(Instant.now());
    }

    @Test
    void getReviewEventById_shouldThrowException_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(reviewEventRepository.getReviewEventById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewEventService.getReviewEventById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getReviewEventById_shouldReturnMappedReviewEvent() {

        when(reviewEventRepository.getReviewEventById(reviewEvent.getId()))
                .thenReturn(Optional.of(reviewEvent));

        EventReviewerBasicInfo reviewerBasicInfo = new EventReviewerBasicInfo(
                UUID.randomUUID(),
                "reviewer"
        );

        ReviewEventResponse mapped = new ReviewEventResponse(
                reviewEvent.getId(),
                reviewEvent.getEventType(),
                reviewEvent.getComment(),
                reviewerBasicInfo,
                reviewEvent.getReview().getId(),
                reviewEvent.getCreatedAt()
        );

        when(reviewEventMapper.toReviewEventResponse(reviewEvent))
                .thenReturn(mapped);

        ReviewEventResponse res = reviewEventService.getReviewEventById(reviewEvent.getId());

        assertThat(res.id()).isEqualTo(mapped.id());
        assertThat(res.eventType()).isEqualTo(mapped.eventType());
        assertThat(res.comment()).isEqualTo(mapped.comment());
        assertThat(res.reviewer()).isEqualTo(mapped.reviewer());
        assertThat(res.createdAt()).isEqualTo(mapped.createdAt());

    }

    @Test
    void getReviewEvents_shouldReturnPaginatedResponse() {
        Page<ReviewEvent> page = new PageImpl<>(List.of(reviewEvent), PageRequest.of(0, 10), 25);
        when(reviewEventRepository.findByReviewId(any(Pageable.class), eq(reviewEvent.getReview().getId())))
                .thenReturn(page);

        EventReviewerBasicInfo reviewerBasicInfo = new EventReviewerBasicInfo(
                UUID.randomUUID(),
                "reviewer"
        );

        ReviewEventResponse mapped = new ReviewEventResponse(
                reviewEvent.getId(),
                reviewEvent.getEventType(),
                reviewEvent.getComment(),
                reviewerBasicInfo,
                reviewEvent.getReview().getId(),
                reviewEvent.getCreatedAt()
        );

        when(reviewEventMapper.toReviewEventResponse(reviewEvent))
                .thenReturn(mapped);

        PagingResult<ReviewEventResponse> response = reviewEventService.getReviewEvents(reviewEvent.getReview().getId(), new PaginationRequest(0, 10, "id", Sort.Direction.DESC), "ALL");

        assertThat(response.content()).contains(mapped);
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(25);
        assertThat(response.last()).isEqualTo(false);
        assertThat(response.next()).isEqualTo(true);
    }

}
