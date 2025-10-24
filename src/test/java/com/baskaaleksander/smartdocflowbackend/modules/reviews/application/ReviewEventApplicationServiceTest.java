package com.baskaaleksander.smartdocflowbackend.modules.reviews.application;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.api.dto.ReviewEventResponse;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.api.mapping.ReviewEventApiMapper;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewEvent;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.port.ReviewEventQueryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewEventApplicationServiceTest {

    @Mock
    private ReviewEventQueryPort reviewEventQueryPort;
    @Mock
    private ReviewEventApiMapper mapper;

    @InjectMocks
    private ReviewEventApplicationService service;

    @Test
    void getReviewEventById_success() {
        UUID id = UUID.randomUUID();
        ReviewEvent event = new ReviewEvent();
        ReviewEventResponse dto = mock(ReviewEventResponse.class);
        when(reviewEventQueryPort.getReviewEventById(id)).thenReturn(Optional.of(event));
        when(mapper.toEventResponse(event)).thenReturn(dto);

        ReviewEventResponse out = service.getReviewEventById(id);

        assertThat(out).isEqualTo(dto);
        verify(reviewEventQueryPort).getReviewEventById(id);
        verify(mapper).toEventResponse(event);
    }

    @Test
    void getReviewEventById_notFound() {
        UUID id = UUID.randomUUID();
        when(reviewEventQueryPort.getReviewEventById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getReviewEventById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getReviewEvents_all_success() {
        UUID reviewId = UUID.randomUUID();
        PaginationRequest req = new PaginationRequest(0, 2, null, null);
        ReviewEvent e1 = new ReviewEvent();
        ReviewEvent e2 = new ReviewEvent();
        PagingResult<ReviewEvent> page = new PagingResult<>(
                List.of(e1, e2), 3, 6L, 2, 0, false, true
        );
        ReviewEventResponse d1 = mock(ReviewEventResponse.class);
        ReviewEventResponse d2 = mock(ReviewEventResponse.class);

        when(reviewEventQueryPort.findByReviewId(req, reviewId)).thenReturn(page);
        when(mapper.toEventResponse(e1)).thenReturn(d1);
        when(mapper.toEventResponse(e2)).thenReturn(d2);

        PagingResult<ReviewEventResponse> out = service.getReviewEvents(reviewId, req, "ALL");

        assertThat(out.content()).containsExactly(d1, d2);
        assertThat(out.totalPages()).isEqualTo(3);
        assertThat(out.totalElements()).isEqualTo(6L);
        assertThat(out.size()).isEqualTo(2);
        assertThat(out.page()).isEqualTo(0);
        assertThat(out.last()).isFalse();
        assertThat(out.next()).isTrue();
        verify(reviewEventQueryPort).findByReviewId(req, reviewId);
        verify(reviewEventQueryPort, never()).findByReviewIdWithType(any(), any(), any());
    }

    @Test
    void getReviewEvents_byType_success() {
        UUID reviewId = UUID.randomUUID();
        PaginationRequest req = new PaginationRequest(1, 3, "createdAt", Sort.Direction.DESC);
        ReviewEvent e1 = new ReviewEvent();
        PagingResult<ReviewEvent> page = new PagingResult<>(
                List.of(e1), 1, 1L, 3, 1, true, false
        );
        ReviewEventResponse d1 = mock(ReviewEventResponse.class);

        when(reviewEventQueryPort.findByReviewIdWithType(eq(req), eq(reviewId), any())).thenReturn(page);
        when(mapper.toEventResponse(e1)).thenReturn(d1);

        PagingResult<ReviewEventResponse> out = service.getReviewEvents(reviewId, req, "COMMENT");

        assertThat(out.content()).containsExactly(d1);
        assertThat(out.totalPages()).isEqualTo(1);
        assertThat(out.totalElements()).isEqualTo(1L);
        assertThat(out.last()).isTrue();
        verify(reviewEventQueryPort).findByReviewIdWithType(eq(req), eq(reviewId), any());
        verify(reviewEventQueryPort, never()).findByReviewId(req, reviewId);
    }
}