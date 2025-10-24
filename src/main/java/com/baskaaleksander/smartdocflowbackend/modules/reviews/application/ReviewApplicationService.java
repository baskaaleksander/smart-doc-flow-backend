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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewApplicationService {

    private final ReviewDomainEventPublisherPort publisher;
    private final ReviewCommandPort reviewCommandPort;
    private final ReviewQueryPort reviewQueryPort;
    private final ReviewEventCommandPort reviewEventCommandPort;
    private final DocumentCommandPort documentCommandPort;
    private final ExternalUserQueryPort externalUserQueryPort;
    private final ReviewApiMapper mapper;
    private final ReviewEventApiMapper eventMapper;

    @Transactional
    public ReviewResponse handleReviewStatusChange(UUID reviewId, String username, ReviewRequest body) {
        if (body.comment() == null) {
            return switch (body.status()) {
                case IN_PROGRESS -> claimReview(reviewId, username);
                case PENDING -> releaseReview(reviewId, username);
                default -> throw new IllegalArgumentException("No action like this possible");
            };
        } else {
            return switch (body.status()) {
                case APPROVED -> approveDocument(reviewId, username, body.comment());
                case REJECTED -> rejectDocument(reviewId, username, body.comment());
                default -> throw new IllegalArgumentException("No action like this possible");
            };
        }
    }

    @Transactional
    public ReviewEventResponse commentReview(String username, String comment, UUID reviewId) {
        Review review = reviewQueryPort.getReviewById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review with ID " + reviewId + " not found"));

        UUID reviewerId = externalUserQueryPort.findIdByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        publisher.publish(new NotificationEvent(username, "review_comment", "Document " + review.getDocumentId() + " received comment"));

        return eventMapper.toEventResponse(logReviewEvent(reviewerId, review.getId(), ReviewEventType.COMMENT, comment));
    }

    private ReviewEvent logReviewEvent(UUID reviewerId, UUID reviewId, ReviewEventType eventType, String comment) {
        ReviewEvent reviewEvent = new ReviewEvent();

        ReviewerBasic reviewer = externalUserQueryPort.findReviewerById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer not found"));

        reviewEvent.setReviewer(reviewer);
        reviewEvent.setReviewId(reviewId);
        reviewEvent.setEventType(eventType);
        if (comment != null) reviewEvent.setComment(comment);

        return reviewEventCommandPort.save(reviewEvent);
    }

    @Transactional
    public ReviewResponse claimReview(UUID reviewId, String username) {
        Review review = reviewQueryPort.getReviewById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review with ID " + reviewId + " not found"));

        if (review.getStatus() != ReviewStatus.PENDING) {
            throw new ResourceConflictException("Review with ID " + reviewId + " is already claimed");
        }

        UUID reviewer = externalUserQueryPort.findIdByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User with username " + username + " not found"));
        review.setReviewerId(reviewer);
        review.setStatus(ReviewStatus.IN_PROGRESS);


        documentCommandPort.updateStatus(review.getDocumentId(), "in_review");

        review = reviewCommandPort.save(review);

        logReviewEvent(reviewer, review.getId(), ReviewEventType.ASSIGNED, null);

        publisher.publish(new NotificationEvent(username, "document_in_review", "Document " + review.getDocumentId() + " is in review process!"));

        return mapper.toReviewResponse(review);
    }

    @Transactional
    public ReviewResponse releaseReview(UUID reviewId, String username) {
        Review review = reviewQueryPort.getReviewById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        UUID reviewerId = review.getReviewerId();
        String reviewerUsername = externalUserQueryPort.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer not found"));

        if (review.getStatus() != ReviewStatus.IN_PROGRESS) {
            throw new ResourceConflictException("Review cannot be released");
        }
        if (!reviewerUsername.equalsIgnoreCase(username)) {
            throw new AccessDeniedException("You are not allowed to release this review");
        }

        review.setReviewerId(null);
        review.setStatus(ReviewStatus.PENDING);
        documentCommandPort.updateStatus(review.getDocumentId(), "review_pending");

        review = reviewCommandPort.save(review);

        logReviewEvent(reviewerId, review.getId(), ReviewEventType.RELEASED, null);

        return mapper.toReviewResponse(review);
    }

    @Transactional
    public ReviewResponse approveDocument(UUID reviewId, String username, String comment) {
        Review review = reviewQueryPort.getReviewById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review with ID " + reviewId + " not found"));

        if (review.getStatus() != ReviewStatus.IN_PROGRESS) {
            throw new ResourceConflictException("Review with ID " + reviewId + " cannot be approved, it needs to be IN_PROGRESS status");
        }

        String reviewer = externalUserQueryPort.findById(review.getReviewerId())
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer not found"));

        if (!reviewer.equalsIgnoreCase(username)) {
            throw new AccessDeniedException("You are not allowed to approve this document");
        }

        review.setComment(comment);
        review.setStatus(ReviewStatus.APPROVED);
        documentCommandPort.updateStatus(review.getDocumentId(), "reviewed");


        review = reviewCommandPort.save(review);

        logReviewEvent(review.getReviewerId(), review.getId(), ReviewEventType.APPROVED, comment);

        publisher.publish(new NotificationEvent(username, "document_reviewed", "Document " + review.getDocumentId() + " is approved."));

        return mapper.toReviewResponse(review);
    }

    @Transactional
    public ReviewResponse rejectDocument(UUID reviewId, String username, String comment) {
        Review review = reviewQueryPort.getReviewById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review with ID " + reviewId + " not found"));

        if (review.getStatus() != ReviewStatus.IN_PROGRESS) {
            throw new ResourceConflictException("Review with ID " + reviewId + " cannot be approved, it needs to be IN_PROGRESS status");
        }

        String reviewer = externalUserQueryPort.findById(review.getReviewerId())
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer not found"));

        if (!reviewer.equalsIgnoreCase(username)) {
            throw new AccessDeniedException("You are not allowed to reject this document");
        }

        review.setStatus(ReviewStatus.REJECTED);
        review.setComment(comment);
        documentCommandPort.updateStatus(review.getDocumentId(), "reviewed");


        review = reviewCommandPort.save(review);

        logReviewEvent(review.getReviewerId(), reviewId, ReviewEventType.REJECTED, comment);

        publisher.publish(new NotificationEvent(username, "document_reviewed", "Document " + review.getDocumentId() + " got rejected."));

        return mapper.toReviewResponse(review);
    }

    public PagingResult<ReviewResponse> getAllReviews(Optional<String> status, PaginationRequest request) {

        PagingResult<Review> reviews = null;

        if (status.isPresent()) {
            String statusVal = status.get();
            reviews = reviewQueryPort.findByStatus(request, ReviewStatus.fromString(statusVal));
        } else {
            reviews = reviewQueryPort.findAll(request);
        }

        List<ReviewResponse> reviewsList = reviews
                .content()
                .stream()
                .map(mapper::toReviewResponse)
                .toList();

        return new PagingResult<>(
                reviewsList,
                reviews.totalPages(),
                reviews.totalElements(),
                reviews.size(),
                reviews.page(),
                reviews.last(),
                reviews.next()
        );
    }

    public ReviewResponse getReviewById(UUID id) {
        return mapper.toReviewResponse(
                reviewQueryPort.getReviewById(id).orElseThrow(() -> new ResourceNotFoundException("Review not found"))
        );
    }

}
