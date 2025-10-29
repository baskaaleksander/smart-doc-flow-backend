package com.baskaaleksander.smartdocflowbackend.modules.reviews.application;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceConflictException;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.logging.LoggingPort;
import com.baskaaleksander.smartdocflowbackend.common.logging.Slf4jLoggingAdapter;
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
    private final LoggingPort logger;

    @Transactional
    public ReviewResponse handleReviewStatusChange(UUID reviewId, String username, ReviewRequest body) {
        logger.info("REVIEW_HANDLE START reviewId=" + Slf4jLoggingAdapter.shortId(reviewId)
                + " username=" + username
                + " status=" + body.status()
                + " hasComment=" + (body.comment() != null));

        ReviewResponse response;
        if (body.comment() == null) {
            switch (body.status()) {
                case IN_PROGRESS -> response = claimReview(reviewId, username);
                case PENDING -> response = releaseReview(reviewId, username);
                default -> {
                    logger.warn("REVIEW_HANDLE FAILED reason=invalid_action reviewId=" + Slf4jLoggingAdapter.shortId(reviewId)
                            + " status=" + body.status());
                    throw new IllegalArgumentException("No action like this possible");
                }
            }
        } else {
            switch (body.status()) {
                case APPROVED -> response = approveDocument(reviewId, username, body.comment());
                case REJECTED -> response = rejectDocument(reviewId, username, body.comment());
                default -> {
                    logger.warn("REVIEW_HANDLE FAILED reason=invalid_action_with_comment reviewId=" + Slf4jLoggingAdapter.shortId(reviewId)
                            + " status=" + body.status());
                    throw new IllegalArgumentException("No action like this possible");
                }
            }
        }

        logger.info("REVIEW_HANDLE SUCCESS reviewId=" + Slf4jLoggingAdapter.shortId(reviewId)
                + " finalStatus=" + response.status());
        return response;
    }

    @Transactional
    public ReviewEventResponse commentReview(String username, String comment, UUID reviewId) {
        logger.info("REVIEW_COMMENT START reviewId=" + Slf4jLoggingAdapter.shortId(reviewId)
                + " username=" + username);

        Review review = reviewQueryPort.getReviewById(reviewId)
                .orElseThrow(() -> {
                    logger.warn("REVIEW_COMMENT FAILED reason=review_not_found reviewId=" + Slf4jLoggingAdapter.shortId(reviewId));
                    return new ResourceNotFoundException("Review with ID " + reviewId + " not found");
                });

        UUID reviewerId = externalUserQueryPort.findIdByUsername(username)
                .orElseThrow(() -> {
                    logger.warn("REVIEW_COMMENT FAILED reason=user_not_found username=" + username);
                    return new ResourceNotFoundException("User not found");
                });

        publisher.publish(new NotificationEvent(username, "review_comment",
                "Document " + review.getDocumentId() + " received comment"));

        ReviewEventResponse resp = eventMapper.toEventResponse(
                logReviewEvent(reviewerId, review.getId(), ReviewEventType.COMMENT, comment)
        );

        logger.info("REVIEW_COMMENT SUCCESS reviewId=" + Slf4jLoggingAdapter.shortId(reviewId)
                + " reviewerId=" + Slf4jLoggingAdapter.shortId(reviewerId));
        return resp;
    }

    private ReviewEvent logReviewEvent(UUID reviewerId, UUID reviewId, ReviewEventType eventType, String comment) {
        logger.info("REVIEW_EVENT START reviewId=" + Slf4jLoggingAdapter.shortId(reviewId)
                + " reviewerId=" + Slf4jLoggingAdapter.shortId(reviewerId)
                + " type=" + eventType);

        ReviewerBasic reviewer = externalUserQueryPort.findReviewerById(reviewerId)
                .orElseThrow(() -> {
                    logger.warn("REVIEW_EVENT FAILED reason=reviewer_not_found reviewerId=" + Slf4jLoggingAdapter.shortId(reviewerId));
                    return new ResourceNotFoundException("Reviewer not found");
                });

        ReviewEvent reviewEvent = new ReviewEvent();
        reviewEvent.setReviewer(reviewer);
        reviewEvent.setReviewId(reviewId);
        reviewEvent.setEventType(eventType);
        if (comment != null) reviewEvent.setComment(comment);

        ReviewEvent saved = reviewEventCommandPort.save(reviewEvent);
        logger.info("REVIEW_EVENT SUCCESS eventId=" + Slf4jLoggingAdapter.shortId(saved.getId())
                + " reviewId=" + Slf4jLoggingAdapter.shortId(reviewId)
                + " type=" + eventType);
        return saved;
    }

    @Transactional
    public ReviewResponse claimReview(UUID reviewId, String username) {
        logger.info("REVIEW_CLAIM START reviewId=" + Slf4jLoggingAdapter.shortId(reviewId)
                + " username=" + username);

        Review review = reviewQueryPort.getReviewById(reviewId)
                .orElseThrow(() -> {
                    logger.warn("REVIEW_CLAIM FAILED reason=review_not_found reviewId=" + Slf4jLoggingAdapter.shortId(reviewId));
                    return new ResourceNotFoundException("Review with ID " + reviewId + " not found");
                });

        if (review.getStatus() != ReviewStatus.PENDING) {
            logger.warn("REVIEW_CLAIM FAILED reason=not_pending reviewId=" + Slf4jLoggingAdapter.shortId(reviewId)
                    + " status=" + review.getStatus());
            throw new ResourceConflictException("Review with ID " + reviewId + " is already claimed");
        }

        UUID reviewer = externalUserQueryPort.findIdByUsername(username)
                .orElseThrow(() -> {
                    logger.warn("REVIEW_CLAIM FAILED reason=reviewer_not_found username=" + username);
                    return new ResourceNotFoundException("User with username " + username + " not found");
                });

        review.setReviewerId(reviewer);
        review.setStatus(ReviewStatus.IN_PROGRESS);
        documentCommandPort.updateStatus(review.getDocumentId(), "in_review");
        review = reviewCommandPort.save(review);

        logReviewEvent(reviewer, review.getId(), ReviewEventType.ASSIGNED, null);
        publisher.publish(new NotificationEvent(username, "document_in_review",
                "Document " + review.getDocumentId() + " is in review process!"));

        logger.info("REVIEW_CLAIM SUCCESS reviewId=" + Slf4jLoggingAdapter.shortId(reviewId)
                + " reviewerId=" + Slf4jLoggingAdapter.shortId(reviewer));
        return mapper.toReviewResponse(review);
    }

    @Transactional
    public ReviewResponse releaseReview(UUID reviewId, String username) {
        logger.info("REVIEW_RELEASE START reviewId=" + Slf4jLoggingAdapter.shortId(reviewId)
                + " username=" + username);

        Review review = reviewQueryPort.getReviewById(reviewId)
                .orElseThrow(() -> {
                    logger.warn("REVIEW_RELEASE FAILED reason=review_not_found reviewId=" + Slf4jLoggingAdapter.shortId(reviewId));
                    return new ResourceNotFoundException("Review not found");
                });

        UUID reviewerId = review.getReviewerId();
        String reviewerUsername = externalUserQueryPort.findById(reviewerId)
                .orElseThrow(() -> {
                    logger.warn("REVIEW_RELEASE FAILED reason=reviewer_not_found reviewerId=" + Slf4jLoggingAdapter.shortId(reviewerId));
                    return new ResourceNotFoundException("Reviewer not found");
                });

        if (review.getStatus() != ReviewStatus.IN_PROGRESS) {
            logger.warn("REVIEW_RELEASE FAILED reason=not_in_progress reviewId=" + Slf4jLoggingAdapter.shortId(reviewId)
                    + " status=" + review.getStatus());
            throw new ResourceConflictException("Review cannot be released");
        }
        if (!reviewerUsername.equalsIgnoreCase(username)) {
            logger.warn("REVIEW_RELEASE FAILED reason=forbidden reviewId=" + Slf4jLoggingAdapter.shortId(reviewId)
                    + " owner=" + reviewerUsername + " username=" + username);
            throw new AccessDeniedException("You are not allowed to release this review");
        }

        review.setReviewerId(null);
        review.setStatus(ReviewStatus.PENDING);
        documentCommandPort.updateStatus(review.getDocumentId(), "review_pending");
        review = reviewCommandPort.save(review);

        logReviewEvent(reviewerId, review.getId(), ReviewEventType.RELEASED, null);

        logger.info("REVIEW_RELEASE SUCCESS reviewId=" + Slf4jLoggingAdapter.shortId(reviewId)
                + " prevReviewerId=" + Slf4jLoggingAdapter.shortId(reviewerId));
        return mapper.toReviewResponse(review);
    }

    @Transactional
    public ReviewResponse approveDocument(UUID reviewId, String username, String comment) {
        logger.info("REVIEW_APPROVE START reviewId=" + Slf4jLoggingAdapter.shortId(reviewId)
                + " username=" + username);

        Review review = reviewQueryPort.getReviewById(reviewId)
                .orElseThrow(() -> {
                    logger.warn("REVIEW_APPROVE FAILED reason=review_not_found reviewId=" + Slf4jLoggingAdapter.shortId(reviewId));
                    return new ResourceNotFoundException("Review with ID " + reviewId + " not found");
                });

        if (review.getStatus() != ReviewStatus.IN_PROGRESS) {
            logger.warn("REVIEW_APPROVE FAILED reason=not_in_progress reviewId=" + Slf4jLoggingAdapter.shortId(reviewId)
                    + " status=" + review.getStatus());
            throw new ResourceConflictException("Review with ID " + reviewId + " cannot be approved, it needs to be IN_PROGRESS status");
        }

        Review finalReview = review;
        String reviewer = externalUserQueryPort.findById(review.getReviewerId())
                .orElseThrow(() -> {
                    logger.warn("REVIEW_APPROVE FAILED reason=reviewer_not_found reviewerId=" + Slf4jLoggingAdapter.shortId(finalReview.getReviewerId()));
                    return new ResourceNotFoundException("Reviewer not found");
                });

        if (!reviewer.equalsIgnoreCase(username)) {
            logger.warn("REVIEW_APPROVE FAILED reason=forbidden reviewId=" + Slf4jLoggingAdapter.shortId(reviewId)
                    + " owner=" + reviewer + " username=" + username);
            throw new AccessDeniedException("You are not allowed to approve this document");
        }

        review.setComment(comment);
        review.setStatus(ReviewStatus.APPROVED);
        documentCommandPort.updateStatus(review.getDocumentId(), "reviewed");
        review = reviewCommandPort.save(review);

        logReviewEvent(review.getReviewerId(), review.getId(), ReviewEventType.APPROVED, comment);
        publisher.publish(new NotificationEvent(username, "document_reviewed",
                "Document " + review.getDocumentId() + " is approved."));

        logger.info("REVIEW_APPROVE SUCCESS reviewId=" + Slf4jLoggingAdapter.shortId(reviewId));
        return mapper.toReviewResponse(review);
    }

    @Transactional
    public ReviewResponse rejectDocument(UUID reviewId, String username, String comment) {
        logger.info("REVIEW_REJECT START reviewId=" + Slf4jLoggingAdapter.shortId(reviewId)
                + " username=" + username);

        Review review = reviewQueryPort.getReviewById(reviewId)
                .orElseThrow(() -> {
                    logger.warn("REVIEW_REJECT FAILED reason=review_not_found reviewId=" + Slf4jLoggingAdapter.shortId(reviewId));
                    return new ResourceNotFoundException("Review with ID " + reviewId + " not found");
                });

        if (review.getStatus() != ReviewStatus.IN_PROGRESS) {
            logger.warn("REVIEW_REJECT FAILED reason=not_in_progress reviewId=" + Slf4jLoggingAdapter.shortId(reviewId)
                    + " status=" + review.getStatus());
            throw new ResourceConflictException("Review with ID " + reviewId + " cannot be approved, it needs to be IN_PROGRESS status");
        }

        Review finalReview = review;
        String reviewer = externalUserQueryPort.findById(review.getReviewerId())
                .orElseThrow(() -> {
                    logger.warn("REVIEW_REJECT FAILED reason=reviewer_not_found reviewerId=" + Slf4jLoggingAdapter.shortId(finalReview.getReviewerId()));
                    return new ResourceNotFoundException("Reviewer not found");
                });

        if (!reviewer.equalsIgnoreCase(username)) {
            logger.warn("REVIEW_REJECT FAILED reason=forbidden reviewId=" + Slf4jLoggingAdapter.shortId(reviewId)
                    + " owner=" + reviewer + " username=" + username);
            throw new AccessDeniedException("You are not allowed to reject this document");
        }

        review.setStatus(ReviewStatus.REJECTED);
        review.setComment(comment);
        documentCommandPort.updateStatus(review.getDocumentId(), "reviewed");
        review = reviewCommandPort.save(review);

        logReviewEvent(review.getReviewerId(), reviewId, ReviewEventType.REJECTED, comment);
        publisher.publish(new NotificationEvent(username, "document_reviewed",
                "Document " + review.getDocumentId() + " got rejected."));

        logger.info("REVIEW_REJECT SUCCESS reviewId=" + Slf4jLoggingAdapter.shortId(reviewId));
        return mapper.toReviewResponse(review);
    }

    public PagingResult<ReviewResponse> getAllReviews(Optional<String> status, PaginationRequest request) {
        logger.info("REVIEW_LIST START page=" + request.getPage() + " size=" + request.getSize()
                + " status=" + status.orElse("ALL"));

        PagingResult<Review> reviews = status.isPresent()
                ? reviewQueryPort.findByStatus(request, ReviewStatus.fromString(status.get()))
                : reviewQueryPort.findAll(request);

        List<ReviewResponse> reviewsList = reviews
                .content()
                .stream()
                .map(mapper::toReviewResponse)
                .toList();

        logger.info("REVIEW_LIST SUCCESS page=" + reviews.page()
                + " size=" + reviews.size()
                + " totalElements=" + reviews.totalElements()
                + " totalPages=" + reviews.totalPages());
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
        logger.info("REVIEW_GET START reviewId=" + Slf4jLoggingAdapter.shortId(id));
        Review review = reviewQueryPort.getReviewById(id)
                .orElseThrow(() -> {
                    logger.warn("REVIEW_GET FAILED reason=not_found reviewId=" + Slf4jLoggingAdapter.shortId(id));
                    return new ResourceNotFoundException("Review not found");
                });
        logger.info("REVIEW_GET SUCCESS reviewId=" + Slf4jLoggingAdapter.shortId(id)
                + " status=" + review.getStatus());
        return mapper.toReviewResponse(review);
    }
}