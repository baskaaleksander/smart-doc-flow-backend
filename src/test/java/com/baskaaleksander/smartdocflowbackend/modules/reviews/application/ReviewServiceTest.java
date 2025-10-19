package com.baskaaleksander.smartdocflowbackend.modules.reviews.application;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceConflictException;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.entity.DocumentEntity;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.spring.SpringDataDocumentRepository;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.application.NotificationApplicationService;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.api.dto.ReviewEventResponse;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.api.dto.ReviewRequest;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.api.dto.ReviewResponse;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewEventType;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewStatus;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewerBasic;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.mapping.ReviewEventMapper;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.mapping.ReviewMapper;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.entity.ReviewEntity;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.entity.ReviewEventEntity;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.spring.SpringDataReviewEventRepository;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.spring.SpringDataReviewRepository;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.entity.UserEntity;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.spring.SpringDataUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class ReviewServiceTest {

    @Mock
    private SpringDataReviewRepository reviewRepository;
    @Mock
    private SpringDataUserRepository userRepository;
    @Mock
    private ReviewMapper reviewMapper;
    @Mock
    private SpringDataDocumentRepository documentRepository;
    @Mock
    private SpringDataReviewEventRepository reviewEventRepository;
    @Mock
    private ReviewEventMapper reviewEventMapper;
    @Mock
    private NotificationApplicationService notificationService;

    @InjectMocks
    private ReviewApplicationService realService;

    private ReviewApplicationService reviewService;
    private UserEntity reviewer;
    private ReviewEntity review;
    private DocumentEntity document;

    @BeforeEach
    void setUp() {
        reviewService = spy(realService);
        reviewer = new UserEntity();
        reviewer.setId(UUID.randomUUID());
        reviewer.setUsername("user-123");

        document = new DocumentEntity();
        document.setId(UUID.randomUUID());

        review = new ReviewEntity();
        review.setId(UUID.randomUUID());
        review.setDocument(document);
    }

    @Test
    void handleReviewStatusChange_shouldThrowExceptionWhileCommentForbidden() {
        ReviewRequest body = new ReviewRequest(ReviewStatus.IN_PROGRESS, "example");

        assertThatThrownBy(() -> reviewService.handleReviewStatusChange(UUID.randomUUID(), "userid", body))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void handleReviewStatusChange_shouldThrowExceptionWhileCommentRequired() {
        ReviewRequest body = new ReviewRequest(ReviewStatus.APPROVED, "");

        assertThatThrownBy(() -> reviewService.handleReviewStatusChange(UUID.randomUUID(), "userid", body))
                .isInstanceOf(IllegalArgumentException.class);

    }

    @Test
    void handleReviewStatusChange_shouldSuccessWithComment() {
        var body = new ReviewRequest(ReviewStatus.APPROVED, "example");


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
        var body = new ReviewRequest(ReviewStatus.IN_PROGRESS, "");

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

        when(reviewRepository.getReviewById(id)).thenReturn(Optional.of(new ReviewEntity()));

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

        when(reviewEventRepository.save(any(ReviewEventEntity.class)))
                .thenAnswer(inv -> {
                    ReviewEventEntity ev = inv.getArgument(0);
                    if (ev.getId() == null) ev.setId(UUID.randomUUID());
                    if (ev.getCreatedAt() == null) ev.setCreatedAt(Instant.now());
                    return ev;
                });

        when(reviewEventMapper.toReviewEventResponse(any(ReviewEventEntity.class)))
                .thenAnswer(inv -> {
                    ReviewEventEntity ev = inv.getArgument(0);

                    ReviewerBasic reviewerBasicInfo = new ReviewerBasic(
                            ev.getReviewer().getId(),
                            ev.getReviewer().getUsername()
                    );
                    return new ReviewEventResponse(
                            ev.getId(),
                            ev.getEventType(),
                            ev.getComment(),
                            reviewerBasicInfo,
                            ev.getReview().getId(),
                            ev.getCreatedAt()
                    );
                });

        ReviewerBasic reviewerBasicInfo = new ReviewerBasic(
                reviewer.getId(),
                reviewer.getUsername()
        );

        ReviewEventResponse res =
                reviewService.commentReview(reviewer.getUsername(), "some comment", review.getId());

        assertThat(res).isNotNull();
        assertThat(res.eventType()).isEqualTo(ReviewEventType.COMMENT);
        assertThat(res.comment()).isEqualTo("some comment");
        assertThat(res.reviewer()).isEqualTo(reviewerBasicInfo);
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

        UserEntity someoneElse = new UserEntity();
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

        when(reviewRepository.save(any(ReviewEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        when(reviewEventRepository.save(any(ReviewEventEntity.class)))
                .thenAnswer(inv -> {
                    ReviewEventEntity ev = inv.getArgument(0);
                    if (ev.getId() == null) ev.setId(UUID.randomUUID());
                    if (ev.getCreatedAt() == null) ev.setCreatedAt(Instant.now());
                    return ev;
                });

        when(reviewMapper.toReviewResponse(any(ReviewEntity.class)))
                .thenAnswer(inv -> {
                    ReviewEntity r = inv.getArgument(0);
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
        verify(reviewRepository).save(any(ReviewEntity.class));
        verify(reviewEventRepository).save(any(ReviewEventEntity.class));
        verify(reviewMapper).toReviewResponse(any(ReviewEntity.class));
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


    @Test
    void approveDocument_shouldReturnReviewResponse_andSendNotification_andLogEvent() {
        review.setStatus(ReviewStatus.IN_PROGRESS);
        review.setReviewer(reviewer);

        when(reviewRepository.getReviewById(review.getId()))
                .thenReturn(Optional.of(review));

        doAnswer(inv -> { review.setStatus(ReviewStatus.APPROVED); return null; })
                .when(reviewRepository).updateStatus(eq(review.getId()), eq(ReviewStatus.APPROVED));

        doNothing().when(documentRepository)
                .updateStatus(eq(document.getId()), eq(DocumentStatus.REVIEWED));

        when(reviewRepository.save(any(ReviewEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        when(reviewEventRepository.save(any(ReviewEventEntity.class)))
                .thenAnswer(inv -> {
                    ReviewEventEntity ev = inv.getArgument(0);
                    if (ev.getId() == null) ev.setId(UUID.randomUUID());
                    if (ev.getCreatedAt() == null) ev.setCreatedAt(Instant.now());
                    return ev;
                });

        when(reviewMapper.toReviewResponse(any(ReviewEntity.class)))
                .thenAnswer(inv -> {
                    ReviewEntity r = inv.getArgument(0);
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

        String comment = "LGTM";
        ReviewResponse res = reviewService.approveDocument(review.getId(), reviewer.getUsername(), comment);

        assertThat(res).isNotNull();
        assertThat(res.id()).isEqualTo(review.getId());
        assertThat(res.documentId()).isEqualTo(document.getId());
        assertThat(res.status()).isEqualTo(ReviewStatus.APPROVED);
        assertThat(res.reviewerId()).isEqualTo(reviewer.getId());
        assertThat(res.comment()).isEqualTo(comment);

        verify(reviewRepository).getReviewById(review.getId());
        verify(reviewRepository).updateStatus(review.getId(), ReviewStatus.APPROVED);
        verify(documentRepository).updateStatus(document.getId(), DocumentStatus.REVIEWED);
        verify(reviewRepository).save(any(ReviewEntity.class));
        verify(reviewMapper).toReviewResponse(any(ReviewEntity.class));

//        verify(notificationService).sendNotification(
//                eq(reviewer.getUsername()),
//                eq("document_reviewed"),
//                contains("is approved")
//        );
    }

    @Test
    void rejectDocument_shouldThrowException_whenReviewNotFound() {
        UUID id = UUID.randomUUID();
        when(reviewRepository.getReviewById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.rejectDocument(id, "user-123", "nope"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rejectDocument_shouldThrowException_whenReviewStatusIsIncorrect() {
        review.setStatus(ReviewStatus.PENDING);
        review.setReviewer(reviewer);

        when(reviewRepository.getReviewById(review.getId()))
                .thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.rejectDocument(review.getId(), reviewer.getUsername(), "nope"))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("needs to be IN_PROGRESS");
    }

    @Test
    void rejectDocument_shouldThrowException_whenUserNotOwner() {
        review.setStatus(ReviewStatus.IN_PROGRESS);
        review.setReviewer(reviewer);

        when(reviewRepository.getReviewById(review.getId()))
                .thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.rejectDocument(review.getId(), "other-user", "nope"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void rejectDocument_shouldReturnReviewResponse_andSendNotification_andLogEvent() {
        review.setStatus(ReviewStatus.IN_PROGRESS);
        review.setReviewer(reviewer);

        when(reviewRepository.getReviewById(review.getId()))
                .thenReturn(Optional.of(review));

        doAnswer(inv -> { review.setStatus(ReviewStatus.REJECTED); return null; })
                .when(reviewRepository).updateStatus(eq(review.getId()), eq(ReviewStatus.REJECTED));

        doNothing().when(documentRepository)
                .updateStatus(eq(document.getId()), eq(DocumentStatus.REVIEWED));

        when(reviewRepository.save(any(ReviewEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        when(reviewEventRepository.save(any(ReviewEventEntity.class)))
                .thenAnswer(inv -> {
                    ReviewEventEntity ev = inv.getArgument(0);
                    if (ev.getId() == null) ev.setId(UUID.randomUUID());
                    if (ev.getCreatedAt() == null) ev.setCreatedAt(Instant.now());
                    return ev;
                });

        when(reviewMapper.toReviewResponse(any(ReviewEntity.class)))
                .thenAnswer(inv -> {
                    ReviewEntity r = inv.getArgument(0);
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

        String comment = "Needs fixes";
        ReviewResponse res = reviewService.rejectDocument(review.getId(), reviewer.getUsername(), comment);

        assertThat(res).isNotNull();
        assertThat(res.id()).isEqualTo(review.getId());
        assertThat(res.documentId()).isEqualTo(document.getId());
        assertThat(res.status()).isEqualTo(ReviewStatus.REJECTED);
        assertThat(res.reviewerId()).isEqualTo(reviewer.getId());
        assertThat(res.comment()).isEqualTo(comment);

        verify(reviewRepository).getReviewById(review.getId());
        verify(reviewRepository).updateStatus(review.getId(), ReviewStatus.REJECTED);
        verify(documentRepository).updateStatus(document.getId(), DocumentStatus.REVIEWED);
        verify(reviewRepository).save(any(ReviewEntity.class));
        verify(reviewMapper).toReviewResponse(any(ReviewEntity.class));


//        verify(notificationService).sendNotification(
//                eq(reviewer.getUsername()),
//                eq("document_reviewed"),
//                contains("got rejected")
//        );
    }

    @Test
    void getAllReviews_shouldReturnPaginatedResponse() {
        Page<ReviewEntity> page = new PageImpl<>(List.of(review), PageRequest.of(0, 10), 25);

        when(reviewRepository.findAll(any(Pageable.class)))
                .thenReturn(page);

        ReviewResponse mapped = new ReviewResponse(
                review.getId(),
                review.getDocument().getId(),
                review.getStatus(),
                review.getReviewer() != null ? review.getReviewer().getId() : null,
                review.getComment(),
                review.getCreatedAt(),
                review.getUpdatedAt(),
                review.getVersion()
        );

        when(reviewMapper.toReviewResponse(review))
                .thenReturn(mapped);

        PagingResult<ReviewResponse> response = reviewService.getAllReviews(Optional.empty(), new PaginationRequest(0, 10, "id", Sort.Direction.DESC));

        assertThat(response.content()).containsExactly(mapped);
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(25);
        assertThat(response.last()).isEqualTo(false);
        assertThat(response.next()).isEqualTo(true);
    }
}
