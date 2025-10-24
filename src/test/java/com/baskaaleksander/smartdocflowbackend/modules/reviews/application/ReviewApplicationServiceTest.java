package com.baskaaleksander.smartdocflowbackend.modules.reviews.application;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceConflictException;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.contracts.NotificationEvent;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.api.dto.ReviewEventResponse;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.api.dto.ReviewRequest;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.api.dto.ReviewResponse;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.api.mapping.ReviewApiMapper;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.api.mapping.ReviewEventApiMapper;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.*;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.port.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewApplicationServiceTest {

    @Mock
    private ReviewDomainEventPublisherPort publisher;
    @Mock
    private ReviewCommandPort reviewCommandPort;
    @Mock
    private ReviewQueryPort reviewQueryPort;
    @Mock
    private ReviewEventCommandPort reviewEventCommandPort;
    @Mock
    private DocumentCommandPort documentCommandPort;
    @Mock
    private ExternalUserQueryPort externalUserQueryPort;
    @Mock
    private ReviewApiMapper mapper;
    @Mock
    private ReviewEventApiMapper eventMapper;

    @InjectMocks
    private ReviewApplicationService service;

    @Test
    void commentReview_success() {
        UUID reviewId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();
        Review review = new Review();
        review.setId(reviewId);
        review.setDocumentId(UUID.randomUUID());
        when(reviewQueryPort.getReviewById(reviewId)).thenReturn(Optional.of(review));
        when(externalUserQueryPort.findIdByUsername("john")).thenReturn(Optional.of(reviewerId));
        ReviewerBasic rb = new ReviewerBasic(reviewerId, "john");
        when(externalUserQueryPort.findReviewerById(reviewerId)).thenReturn(Optional.of(rb));
        ReviewEvent savedEvent = new ReviewEvent();
        when(reviewEventCommandPort.save(any(ReviewEvent.class))).thenReturn(savedEvent);
        ReviewEventResponse dto = mock(ReviewEventResponse.class);
        when(eventMapper.toEventResponse(savedEvent)).thenReturn(dto);

        ReviewEventResponse out = service.commentReview("john", "Looks good", reviewId);

        assertThat(out).isEqualTo(dto);
        verify(publisher).publish(argThat(e ->
                e instanceof NotificationEvent ne && ne.type().equals("review_comment")));
        verify(reviewEventCommandPort).save(argThat(ev ->
                ev.getEventType() == ReviewEventType.COMMENT && "Looks good".equals(ev.getComment())));
    }

    @Test
    void claimReview_success() {
        UUID reviewId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();

        Review review = new Review();
        review.setId(reviewId);
        review.setDocumentId(docId);
        review.setStatus(ReviewStatus.PENDING);
        when(reviewQueryPort.getReviewById(reviewId)).thenReturn(Optional.of(review));
        when(externalUserQueryPort.findIdByUsername("john")).thenReturn(Optional.of(reviewerId));

        when(reviewCommandPort.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reviewEventCommandPort.save(any(ReviewEvent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(externalUserQueryPort.findReviewerById(reviewerId)).thenReturn(Optional.of(new ReviewerBasic(reviewerId, "john")));

        ReviewResponse dto = mock(ReviewResponse.class);
        when(mapper.toReviewResponse(any(Review.class))).thenReturn(dto);

        ReviewResponse out = service.claimReview(reviewId, "john");

        assertThat(out).isEqualTo(dto);
        verify(documentCommandPort).updateStatus(docId, "in_review");
        verify(reviewCommandPort).save(argThat(r ->
                r.getStatus() == ReviewStatus.IN_PROGRESS && reviewerId.equals(r.getReviewerId())));
        verify(publisher).publish(argThat(e ->
                e instanceof NotificationEvent ne && ne.type().equals("document_in_review")));
    }

    @Test
    void claimReview_conflictWhenNotPending() {
        UUID reviewId = UUID.randomUUID();
        Review review = new Review();
        review.setId(reviewId);
        review.setStatus(ReviewStatus.IN_PROGRESS);
        when(reviewQueryPort.getReviewById(reviewId)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> service.claimReview(reviewId, "john"))
                .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void releaseReview_success() {
        UUID reviewId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();
        Review review = new Review();
        review.setId(reviewId);
        review.setDocumentId(docId);
        review.setReviewerId(reviewerId);
        review.setStatus(ReviewStatus.IN_PROGRESS);

        when(reviewQueryPort.getReviewById(reviewId)).thenReturn(Optional.of(review));
        when(externalUserQueryPort.findById(reviewerId)).thenReturn(Optional.of("john"));
        when(reviewCommandPort.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reviewEventCommandPort.save(any(ReviewEvent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(externalUserQueryPort.findReviewerById(reviewerId)).thenReturn(Optional.of(new ReviewerBasic(reviewerId, "john")));
        ReviewResponse dto = mock(ReviewResponse.class);
        when(mapper.toReviewResponse(any(Review.class))).thenReturn(dto);

        ReviewResponse out = service.releaseReview(reviewId, "john");

        assertThat(out).isEqualTo(dto);
        verify(documentCommandPort).updateStatus(docId, "review_pending");
        verify(reviewCommandPort).save(argThat(r ->
                r.getStatus() == ReviewStatus.PENDING && r.getReviewerId() == null));
        verify(reviewEventCommandPort).save(argThat(ev -> ev.getEventType() == ReviewEventType.RELEASED));
    }

    @Test
    void releaseReview_wrongUser_forbidden() {
        UUID reviewId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();
        Review review = new Review();
        review.setId(reviewId);
        review.setReviewerId(reviewerId);
        review.setStatus(ReviewStatus.IN_PROGRESS);
        when(reviewQueryPort.getReviewById(reviewId)).thenReturn(Optional.of(review));
        when(externalUserQueryPort.findById(reviewerId)).thenReturn(Optional.of("kate"));

        assertThatThrownBy(() -> service.releaseReview(reviewId, "john"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void approveDocument_success() {
        UUID reviewId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();
        Review review = new Review();
        review.setId(reviewId);
        review.setDocumentId(docId);
        review.setReviewerId(reviewerId);
        review.setStatus(ReviewStatus.IN_PROGRESS);

        when(reviewQueryPort.getReviewById(reviewId)).thenReturn(Optional.of(review));
        when(externalUserQueryPort.findById(reviewerId)).thenReturn(Optional.of("john"));
        when(reviewCommandPort.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reviewEventCommandPort.save(any(ReviewEvent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(externalUserQueryPort.findReviewerById(reviewerId)).thenReturn(Optional.of(new ReviewerBasic(reviewerId, "john")));
        ReviewResponse dto = mock(ReviewResponse.class);
        when(mapper.toReviewResponse(any(Review.class))).thenReturn(dto);

        ReviewResponse out = service.approveDocument(reviewId, "john", "ok");

        assertThat(out).isEqualTo(dto);
        verify(documentCommandPort).updateStatus(docId, "reviewed");
        verify(reviewCommandPort).save(argThat(r ->
                r.getStatus() == ReviewStatus.APPROVED && "ok".equals(r.getComment())));
        verify(reviewEventCommandPort).save(argThat(ev -> ev.getEventType() == ReviewEventType.APPROVED));
        verify(publisher).publish(argThat(e ->
                e instanceof NotificationEvent ne && ne.type().equals("document_reviewed")));
    }

    @Test
    void approveDocument_wrongStatus_conflict() {
        UUID rid = UUID.randomUUID();
        Review review = new Review();
        review.setId(rid);
        review.setStatus(ReviewStatus.PENDING);
        when(reviewQueryPort.getReviewById(rid)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> service.approveDocument(rid, "john", "x"))
                .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void approveDocument_wrongUser_forbidden() {
        UUID rid = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();
        Review review = new Review();
        review.setId(rid);
        review.setReviewerId(reviewerId);
        review.setStatus(ReviewStatus.IN_PROGRESS);
        when(reviewQueryPort.getReviewById(rid)).thenReturn(Optional.of(review));
        when(externalUserQueryPort.findById(reviewerId)).thenReturn(Optional.of("kate"));

        assertThatThrownBy(() -> service.approveDocument(rid, "john", "x"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectDocument_success() {
        UUID reviewId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        Review review = new Review();
        review.setId(reviewId);
        review.setReviewerId(reviewerId);
        review.setDocumentId(docId);
        review.setStatus(ReviewStatus.IN_PROGRESS);

        when(reviewQueryPort.getReviewById(reviewId)).thenReturn(Optional.of(review));
        when(externalUserQueryPort.findById(reviewerId)).thenReturn(Optional.of("john"));
        when(reviewCommandPort.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reviewEventCommandPort.save(any(ReviewEvent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(externalUserQueryPort.findReviewerById(reviewerId)).thenReturn(Optional.of(new ReviewerBasic(reviewerId, "john")));
        ReviewResponse dto = mock(ReviewResponse.class);
        when(mapper.toReviewResponse(any(Review.class))).thenReturn(dto);

        ReviewResponse out = service.rejectDocument(reviewId, "john", "nope");

        assertThat(out).isEqualTo(dto);
        verify(documentCommandPort).updateStatus(docId, "reviewed");
        verify(reviewCommandPort).save(argThat(r ->
                r.getStatus() == ReviewStatus.REJECTED && "nope".equals(r.getComment())));
        verify(reviewEventCommandPort).save(argThat(ev -> ev.getEventType() == ReviewEventType.REJECTED));
        verify(publisher).publish(argThat(e ->
                e instanceof NotificationEvent ne && ne.type().equals("document_reviewed")));
    }

    @Test
    void handleReviewStatusChange_switchesCorrectly() {
        UUID reviewId = UUID.randomUUID();
        ReviewRequest inProgress = new ReviewRequest(ReviewStatus.IN_PROGRESS, null);
        ReviewRequest pending = new ReviewRequest(ReviewStatus.PENDING, null);
        ReviewRequest approve = new ReviewRequest(ReviewStatus.APPROVED, "ok");
        ReviewRequest reject = new ReviewRequest(ReviewStatus.REJECTED, "no");

        ReviewResponse r1 = mock(ReviewResponse.class);
        ReviewResponse r2 = mock(ReviewResponse.class);
        ReviewResponse r3 = mock(ReviewResponse.class);
        ReviewResponse r4 = mock(ReviewResponse.class);

        ReviewApplicationService spy = Mockito.spy(service);
        doReturn(r1).when(spy).claimReview(reviewId, "john");
        doReturn(r2).when(spy).releaseReview(reviewId, "john");
        doReturn(r3).when(spy).approveDocument(reviewId, "john", "ok");
        doReturn(r4).when(spy).rejectDocument(reviewId, "john", "no");

        assertThat(spy.handleReviewStatusChange(reviewId, "john", inProgress)).isEqualTo(r1);
        assertThat(spy.handleReviewStatusChange(reviewId, "john", pending)).isEqualTo(r2);
        assertThat(spy.handleReviewStatusChange(reviewId, "john", approve)).isEqualTo(r3);
        assertThat(spy.handleReviewStatusChange(reviewId, "john", reject)).isEqualTo(r4);
    }

    @Test
    void getAllReviews_statusPresent() {
        PaginationRequest req = new PaginationRequest(0, 2, null, null);
        Review r1 = new Review();
        Review r2 = new Review();
        PagingResult<Review> page = new PagingResult<>(List.of(r1, r2), 2, 4L, 2, 0, false, true);
        when(reviewQueryPort.findByStatus(eq(req), any())).thenReturn(page);
        ReviewResponse d1 = mock(ReviewResponse.class);
        ReviewResponse d2 = mock(ReviewResponse.class);
        when(mapper.toReviewResponse(r1)).thenReturn(d1);
        when(mapper.toReviewResponse(r2)).thenReturn(d2);

        PagingResult<ReviewResponse> out = service.getAllReviews(Optional.of("PENDING"), req);

        assertThat(out.content()).containsExactly(d1, d2);
        assertThat(out.totalElements()).isEqualTo(4L);
        verify(reviewQueryPort).findByStatus(eq(req), any());
        verify(reviewQueryPort, never()).findAll(any());
    }

    @Test
    void getAllReviews_all() {
        PaginationRequest req = new PaginationRequest(1, 3, "createdAt,desc", null);
        Review r1 = new Review();
        PagingResult<Review> page = new PagingResult<>(List.of(r1), 1, 1L, 3, 1, true, false);
        when(reviewQueryPort.findAll(req)).thenReturn(page);
        ReviewResponse d1 = mock(ReviewResponse.class);
        when(mapper.toReviewResponse(r1)).thenReturn(d1);

        PagingResult<ReviewResponse> out = service.getAllReviews(Optional.empty(), req);

        assertThat(out.content()).containsExactly(d1);
        assertThat(out.last()).isTrue();
        verify(reviewQueryPort).findAll(req);
    }

    @Test
    void getReviewById_success() {
        UUID id = UUID.randomUUID();
        Review r = new Review();
        when(reviewQueryPort.getReviewById(id)).thenReturn(Optional.of(r));
        ReviewResponse dto = mock(ReviewResponse.class);
        when(mapper.toReviewResponse(r)).thenReturn(dto);

        ReviewResponse out = service.getReviewById(id);

        assertThat(out).isEqualTo(dto);
    }

    @Test
    void getReviewById_notFound() {
        UUID id = UUID.randomUUID();
        when(reviewQueryPort.getReviewById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getReviewById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}