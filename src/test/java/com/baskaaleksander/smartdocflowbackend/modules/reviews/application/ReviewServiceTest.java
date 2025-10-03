package com.baskaaleksander.smartdocflowbackend.modules.reviews.application;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.DocumentRepository;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.application.NotificationService;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.api.dto.ReviewRequest;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.api.dto.ReviewResponse;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.ReviewStatus;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.mapping.ReviewEventMapper;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.mapping.ReviewMapper;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.persistence.Review;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.persistence.ReviewEventRepository;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.persistence.ReviewRepository;
import com.baskaaleksander.smartdocflowbackend.modules.users.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
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

    @BeforeEach
    void setUp() {
        reviewService = spy(realService);
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
}
