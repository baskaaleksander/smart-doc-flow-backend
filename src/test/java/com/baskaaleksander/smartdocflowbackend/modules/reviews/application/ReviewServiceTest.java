package com.baskaaleksander.smartdocflowbackend.modules.reviews.application;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceConflictException;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.Document;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.DocumentRepository;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.application.NotificationService;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.api.dto.ReviewEventResponse;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.api.dto.ReviewRequest;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.api.dto.ReviewResponse;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.ReviewEventType;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.ReviewStatus;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.mapping.ReviewEventMapper;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.mapping.ReviewMapper;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.persistence.Review;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.persistence.ReviewEvent;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.persistence.ReviewEventRepository;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.persistence.ReviewRepository;
import com.baskaaleksander.smartdocflowbackend.modules.users.persistence.User;
import com.baskaaleksander.smartdocflowbackend.modules.users.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ReviewMapper reviewMapper;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private ReviewEventRepository reviewEventRepository;
    @Mock
    private ReviewEventMapper reviewEventMapper;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ReviewService realService;

    private ReviewService reviewService;
    private User reviewer;
    private Review review;
    private Document document;

    @BeforeEach
    void setUp() {
        reviewService = spy(realService);
        reviewer = new User();
        reviewer.setId(UUID.randomUUID());
        reviewer.setUsername("user-123");

        document = new Document();
        document.setId(UUID.randomUUID());

        review = new Review();
        review.setId(UUID.randomUUID());
        review.setDocument(document);
    }

    @Test
    void handleReviewStatusChange_shouldThrowExceptionWhileCommentForbidden() {
        ReviewRequest body = new ReviewRequest();
        body.setStatus(ReviewStatus.IN_PROGRESS);
        body.setComment("example");

        assertThatThrownBy(() -> reviewService.handleReviewStatusChange(UUID.randomUUID(), "userid", body))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void handleReviewStatusChange_shouldThrowExceptionWhileCommentRequired() {
        ReviewRequest body = new ReviewRequest();
        body.setStatus(ReviewStatus.APPROVED);

        assertThatThrownBy(() -> reviewService.handleReviewStatusChange(UUID.randomUUID(), "userid", body))
                .isInstanceOf(IllegalArgumentException.class);

    }

    @Test
    void handleReviewStatusChange_shouldSuccessWithComment() {
        var body = new ReviewRequest();
        body.setStatus(ReviewStatus.APPROVED);
        body.setComment("example");

        UUID id = UUID.randomUUID();
        var response = new ReviewResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                ReviewStatus.APPROVED,
                UUID.randomUUID(),
                "example",
                Instant.now(),
                Instant.now(),
                0
        );

        doReturn(response).when(reviewService)
                .approveDocument(eq(id), eq("userid"), eq("example"));

        reviewService.handleReviewStatusChange(id, "userid", body);

        verify(reviewService).approveDocument(id, "userid", "example");
    }

    @Test
    void handleReviewStatusChange_shouldSuccessWithoutComment() {
        var body = new ReviewRequest();
        body.setStatus(ReviewStatus.IN_PROGRESS);

        UUID id = UUID.randomUUID();
        var response = new ReviewResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                ReviewStatus.IN_PROGRESS,
                UUID.randomUUID(),
                null,
                Instant.now(),
                Instant.now(),
                0
        );

        doReturn(response).when(reviewService)
                .claimReview(eq(id), eq("userid"));

        reviewService.handleReviewStatusChange(id, "userid", body);

        verify(reviewService).claimReview(id, "userid");
    }

    @Test
    void commentReview_shouldThrowException_whenReviewNotFound() {
        UUID id = UUID.randomUUID();

        when(reviewRepository.getReviewById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.commentReview("user-123", "some comment", id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void commentReview_shouldThrowException_whenReviewerNotFound() {
        UUID id = UUID.randomUUID();

        when(reviewRepository.getReviewById(id)).thenReturn(Optional.of(new Review()));

        when(userRepository.findByUsername("user-123"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.commentReview("user-123", "some comment", id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void commentReview_shouldReturnReviewEventResponse() {

        when(reviewRepository.getReviewById(review.getId()))
                .thenReturn(Optional.of(review));
        when(userRepository.findByUsername(reviewer.getUsername()))
                .thenReturn(Optional.of(reviewer));

        when(reviewEventRepository.save(any(ReviewEvent.class)))
                .thenAnswer(inv -> {
                    ReviewEvent ev = inv.getArgument(0);
                    if (ev.getId() == null) ev.setId(UUID.randomUUID());
                    if (ev.getCreatedAt() == null) ev.setCreatedAt(Instant.now());
                    return ev;
                });

        when(reviewEventMapper.toReviewEventResponse(any(ReviewEvent.class)))
                .thenAnswer(inv -> {
                    ReviewEvent ev = inv.getArgument(0);
                    return new ReviewEventResponse(
                            ev.getId(),
                            ev.getEventType(),
                            ev.getComment(),
                            ev.getReviewer().getId(),
                            ev.getCreatedAt()
                    );
                });

        ReviewEventResponse res =
                reviewService.commentReview(reviewer.getUsername(), "some comment", review.getId());

        assertThat(res).isNotNull();
        assertThat(res.eventType()).isEqualTo(ReviewEventType.COMMENT);
        assertThat(res.comment()).isEqualTo("some comment");
        assertThat(res.reviewerId()).isEqualTo(reviewer.getId());
        assertThat(res.id()).isNotNull();
        assertThat(res.createdAt()).isNotNull();
    }

    @Test
    void getReviewById_shouldThrowError_whenReviewNotFound() {
        UUID id = UUID.randomUUID();
        when(reviewRepository.getReviewById(id))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.getReviewById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getReviewById_shouldReturnReviewResponse() {
        when(reviewRepository.getReviewById(review.getId()))
                .thenReturn(Optional.of(review));

        ReviewResponse mapped = new ReviewResponse(
                review.getId(),
                document.getId(),
                ReviewStatus.IN_PROGRESS,
                reviewer.getId(),
                null,
                Instant.now(),
                Instant.now(),
                0
        );

        when(reviewMapper.toReviewResponse(review))
                .thenReturn(mapped);

        ReviewResponse res = reviewService.getReviewById(review.getId());

        assertThat(res.id()).isEqualTo(mapped.id());
        assertThat(res.documentId()).isEqualTo(mapped.documentId());
        assertThat(res.status()).isEqualTo(mapped.status());
        assertThat(res.reviewerId()).isEqualTo(mapped.reviewerId());
        assertThat(res.comment()).isEqualTo(mapped.comment());
        assertThat(res.createdAt()).isEqualTo(mapped.createdAt());
        assertThat(res.updatedAt()).isEqualTo(mapped.updatedAt());
        assertThat(res.version()).isEqualTo(mapped.version());
    }

    @Test
    void claimReview_shouldThrowException_whenReviewNotFound() {
        UUID id = UUID.randomUUID();
        when(reviewRepository.getReviewById(id))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.claimReview(id, "user-123"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void claimReview_shouldThrowException_whenReviewStatusIsIncorrect() {
        review.setStatus(ReviewStatus.IN_PROGRESS);

        when(reviewRepository.getReviewById(review.getId()))
                .thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.claimReview(review.getId(), reviewer.getUsername()))
                .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void claimReview_shouldThrowException_whenReviewerNotFound() {
        review.setStatus(ReviewStatus.PENDING);

        when(reviewRepository.getReviewById(review.getId()))
                .thenReturn(Optional.of(review));

        when(userRepository.findByUsername(reviewer.getUsername()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.claimReview(review.getId(), reviewer.getUsername()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void claimReview_shouldReturnReviewResponse() {
        review.setStatus(ReviewStatus.PENDING);

        when(reviewRepository.getReviewById(review.getId()))
                .thenReturn(Optional.of(review));

        when(userRepository.findByUsername(reviewer.getUsername()))
                .thenReturn(Optional.of(reviewer));


        when(reviewRepository.save(review))
                .thenReturn(review);

        ReviewResponse mapped = new ReviewResponse(
                review.getId(),
                document.getId(),
                review.getStatus(),
                reviewer.getId(),
                null,
                Instant.now(),
                Instant.now(),
                0
        );

        when(reviewMapper.toReviewResponse(review))
                .thenReturn(mapped);

        ReviewResponse res = reviewService.claimReview(review.getId(), reviewer.getUsername());

        assertThat(res.id()).isEqualTo(mapped.id());
        assertThat(res.documentId()).isEqualTo(mapped.documentId());
        assertThat(res.status()).isEqualTo(mapped.status());
        assertThat(res.reviewerId()).isEqualTo(mapped.reviewerId());
        assertThat(res.comment()).isEqualTo(mapped.comment());
        assertThat(res.createdAt()).isEqualTo(mapped.createdAt());
        assertThat(res.updatedAt()).isEqualTo(mapped.updatedAt());
        assertThat(res.version()).isEqualTo(mapped.version());
    }

    @Test
    void releaseReview_shouldThrowException_whenReviewNotFound() {
        UUID id = UUID.randomUUID();
        when(reviewRepository.getReviewById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.releaseReview(id, "user-123"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void releaseReview_shouldThrowException_whenReviewStatusIsIncorrect() {
        review.setStatus(ReviewStatus.PENDING);
        review.setReviewer(reviewer);

        when(reviewRepository.getReviewById(review.getId()))
                .thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.releaseReview(review.getId(), reviewer.getUsername()))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("cannot be released");
    }

    @Test
    void releaseReview_shouldThrowException_whenUserNotOwner() {
        review.setStatus(ReviewStatus.IN_PROGRESS);

        User someoneElse = new User();
        someoneElse.setId(UUID.randomUUID());
        someoneElse.setUsername("not-the-owner");

        review.setReviewer(reviewer);

        when(reviewRepository.getReviewById(review.getId()))
                .thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.releaseReview(review.getId(), someoneElse.getUsername()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void releaseReview_shouldReturnReviewResponse() {
        review.setStatus(ReviewStatus.IN_PROGRESS);
        review.setReviewer(reviewer);

        when(reviewRepository.getReviewById(review.getId()))
                .thenReturn(Optional.of(review));

        doAnswer(inv -> {
            review.setStatus(ReviewStatus.PENDING);
            return null;
        }).when(reviewRepository).updateStatus(eq(review.getId()), eq(ReviewStatus.PENDING));

        doNothing().when(documentRepository)
                .updateStatus(eq(document.getId()), eq(DocumentStatus.REVIEW_PENDING));

        when(reviewRepository.save(any(Review.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        when(reviewEventRepository.save(any(ReviewEvent.class)))
                .thenAnswer(inv -> {
                    ReviewEvent ev = inv.getArgument(0);
                    if (ev.getId() == null) ev.setId(UUID.randomUUID());
                    if (ev.getCreatedAt() == null) ev.setCreatedAt(Instant.now());
                    return ev;
                });

        when(reviewMapper.toReviewResponse(any(Review.class)))
                .thenAnswer(inv -> {
                    Review r = inv.getArgument(0);
                    return new ReviewResponse(
                            r.getId(),
                            r.getDocument().getId(),
                            r.getStatus(),
                            r.getReviewer() != null ? r.getReviewer().getId() : null,
                            r.getComment(),
                            r.getCreatedAt(),
                            r.getUpdatedAt(),
                            r.getVersion()
                    );
                });

        ReviewResponse res = reviewService.releaseReview(review.getId(), reviewer.getUsername());

        assertThat(res).isNotNull();
        assertThat(res.id()).isEqualTo(review.getId());
        assertThat(res.documentId()).isEqualTo(document.getId());
        assertThat(res.status()).isEqualTo(ReviewStatus.PENDING);
        assertThat(res.reviewerId()).isNull();

        verify(reviewRepository).getReviewById(review.getId());
        verify(reviewRepository).updateStatus(review.getId(), ReviewStatus.PENDING);
        verify(documentRepository).updateStatus(document.getId(), DocumentStatus.REVIEW_PENDING);
        verify(reviewRepository).save(any(Review.class));
        verify(reviewEventRepository).save(any(ReviewEvent.class));
        verify(reviewMapper).toReviewResponse(any(Review.class));
    }

    @Test
    void approveDocument_shouldThrowException_whenReviewNotFound() {
        UUID id = UUID.randomUUID();
        when(reviewRepository.getReviewById(id)).thenReturn(Optional.empty());


        assertThatThrownBy(() -> reviewService.approveDocument(id, "user-123", "ok"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void approveDocument_shouldThrowException_whenReviewStatusIsIncorrect() {
        review.setStatus(ReviewStatus.PENDING);
        review.setReviewer(reviewer);

        when(reviewRepository.getReviewById(review.getId()))
                .thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.approveDocument(review.getId(), reviewer.getUsername(), "ok"))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("needs to be IN_PROGRESS");
    }

    @Test
    void approveDocument_shouldThrowException_whenUserNotOwner() {
        review.setStatus(ReviewStatus.IN_PROGRESS);
        review.setReviewer(reviewer);

        when(reviewRepository.getReviewById(review.getId()))
                .thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.approveDocument(review.getId(), "other-user", "ok"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not allowed");
    }
}
