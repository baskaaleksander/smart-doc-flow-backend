package com.baskaaleksander.smartdocflowbackend.modules.reviews.application;

import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.NotificationEvent;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.api.dto.ReviewRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.api.dto.ReviewEventResponse;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.api.dto.ReviewResponse;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.ReviewEventType;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.ReviewStatus;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceConflictException;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.mapping.ReviewEventMapper;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.mapping.ReviewMapper;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.persistence.Review;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.persistence.ReviewEvent;
import com.baskaaleksander.smartdocflowbackend.modules.users.persistence.User;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.DocumentRepository;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.persistence.ReviewEventRepository;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.persistence.ReviewRepository;
import com.baskaaleksander.smartdocflowbackend.modules.users.persistence.UserRepository;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;
    private final DocumentRepository documentRepository;
    private final ReviewEventRepository reviewEventRepository;
    private final ReviewEventMapper reviewEventMapper;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public ReviewService(
            ReviewRepository reviewRepository,
            UserRepository userRepository,
            ReviewMapper reviewMapper,
            DocumentRepository documentRepository,
            ReviewEventRepository reviewEventRepository,
            ReviewEventMapper reviewEventMapper,
            ApplicationEventPublisher publisher
            ) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.reviewMapper = reviewMapper;
        this.documentRepository = documentRepository;
        this.reviewEventRepository = reviewEventRepository;
        this.reviewEventMapper = reviewEventMapper;
        this.publisher = publisher;
    }

    @Transactional
    public ReviewResponse handleReviewStatusChange(UUID reviewId, String username, ReviewRequest body) {
        if(body.getComment().isBlank()) {
            return switch (body.getStatus()) {
                case IN_PROGRESS -> claimReview(reviewId, username);
                case PENDING -> releaseReview(reviewId, username);
                default -> throw new IllegalArgumentException("No action like this possible");
            };
        } else {
            return switch(body.getStatus()) {
                case APPROVED -> approveDocument(reviewId, username, body.getComment());
                case REJECTED -> rejectDocument(reviewId, username, body.getComment());
                default -> throw new IllegalArgumentException("No action like this possible");
            };
        }
    }

    @Transactional
    public ReviewEventResponse commentReview(String username, String comment, UUID reviewId) {
        Review review = reviewRepository.getReviewById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review with ID " + reviewId + " not found"));

        User reviewer = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        publisher.publishEvent(new NotificationEvent(username, "review_comment", "Document " + review.getDocument().getId() + " received comment"));

        return reviewEventMapper.toReviewEventResponse(logReviewEvent(reviewer, review, ReviewEventType.COMMENT, comment));
    }

    private ReviewEvent logReviewEvent(User reviewer, Review review, ReviewEventType eventType, String comment) {
        ReviewEvent reviewEvent = new ReviewEvent();

        reviewEvent.setReviewer(reviewer);
        reviewEvent.setReview(review);
        reviewEvent.setEventType(eventType);
        if (comment != null) reviewEvent.setComment(comment);

        return reviewEventRepository.save(reviewEvent);
    }

    @Transactional
    public ReviewResponse claimReview(UUID reviewId, String username) {
        Review review = reviewRepository.getReviewById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review with ID " + reviewId + " not found"));

        if(review.getStatus() != ReviewStatus.PENDING) {
            throw new ResourceConflictException("Review with ID " + reviewId + " is already claimed");
        }

        User reviewer = userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User with username " + username + " not found"));
        review.setReviewer(reviewer);

        reviewRepository.updateStatus(review.getId(), ReviewStatus.IN_PROGRESS);

        documentRepository.updateStatus(review.getDocument().getId(), DocumentStatus.IN_REVIEW);

        review = reviewRepository.save(review);

        logReviewEvent(reviewer, review,  ReviewEventType.ASSIGNED, null);

        publisher.publishEvent(new NotificationEvent(username, "document_in_review", "Document " + review.getDocument().getId() + " is in review process!"));

        return reviewMapper.toReviewResponse(review);
    }

    @Transactional
    public ReviewResponse releaseReview(UUID reviewId, String username) {
        Review review = reviewRepository.getReviewById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review with ID " + reviewId + " not found"));

        User reviewer = review.getReviewer();

        if(review.getStatus() != ReviewStatus.IN_PROGRESS) {
            throw new ResourceConflictException("Review with ID " + reviewId + " cannot be released");
        }

        if(!reviewer.getUsername().equalsIgnoreCase(username)) {
            throw new AccessDeniedException("You are not allowed to release this review");
        }

        review.setReviewer(null);

        reviewRepository.updateStatus(review.getId(), ReviewStatus.PENDING);
        documentRepository.updateStatus(review.getDocument().getId(), DocumentStatus.REVIEW_PENDING);


        review = reviewRepository.save(review);

        logReviewEvent(reviewer, review,  ReviewEventType.RELEASED, null);


        return reviewMapper.toReviewResponse(review);

    }

    @Transactional
    public ReviewResponse approveDocument(UUID reviewId, String username, String comment) {
        Review review = reviewRepository.getReviewById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review with ID " + reviewId + " not found"));

        if(review.getStatus() != ReviewStatus.IN_PROGRESS) {
            throw new ResourceConflictException("Review with ID " + reviewId + " cannot be approved, it needs to be IN_PROGRESS status");
        }
        User reviewer = review.getReviewer();


        if(!reviewer.getUsername().equalsIgnoreCase(username)) {
            throw new AccessDeniedException("You are not allowed to approve this document");
        }

        review.setComment(comment);
        reviewRepository.updateStatus(reviewId, ReviewStatus.APPROVED);
        documentRepository.updateStatus(review.getDocument().getId(), DocumentStatus.REVIEWED);


        review = reviewRepository.save(review);

        logReviewEvent(reviewer, review,  ReviewEventType.APPROVED, comment);

        publisher.publishEvent(new NotificationEvent(username, "document_reviewed", "Document " + review.getDocument().getId() + " is approved."));

        return reviewMapper.toReviewResponse(review);
    }

    @Transactional
    public ReviewResponse rejectDocument(UUID reviewId, String username, String comment) {
        Review review = reviewRepository.getReviewById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review with ID " + reviewId + " not found"));

        if(review.getStatus() != ReviewStatus.IN_PROGRESS) {
            throw new ResourceConflictException("Review with ID " + reviewId + " cannot be approved, it needs to be IN_PROGRESS status");
        }

        User reviewer = review.getReviewer();

        if(!reviewer.getUsername().equalsIgnoreCase(username)) {
            throw new AccessDeniedException("You are not allowed to reject this document");
        }

        review.setComment(comment);
        reviewRepository.updateStatus(reviewId, ReviewStatus.REJECTED);
        documentRepository.updateStatus(review.getDocument().getId(), DocumentStatus.REVIEWED);


        review = reviewRepository.save(review);

        logReviewEvent(reviewer, review,  ReviewEventType.REJECTED, comment);

        publisher.publishEvent(new NotificationEvent(username, "document_reviewed", "Document " + review.getDocument().getId() + " got rejected."));

        return reviewMapper.toReviewResponse(review);
    }

    public PagingResult<ReviewResponse> getAllReviews(Optional<String> status, PaginationRequest request) {
        Pageable pageable = PaginationUtil.getPageable(request);
        Page<Review> reviews = null;

        if (status.isPresent()) {
            String statusVal = status.get();
            reviews = reviewRepository.findByStatus(pageable, ReviewStatus.fromString(statusVal));
        } else {
            reviews = reviewRepository.findAll(pageable);
        }

        List<ReviewResponse> reviewsList = reviews
                .stream()
                .map(reviewMapper::toReviewResponse)
                .toList();

        Integer currentPage = request.getPage();
        int totalPages = reviews.getTotalPages();

        return new PagingResult<>(
                reviewsList,
                totalPages,
                reviews.getTotalElements(),
                reviews.getSize(),
                reviews.getNumber(),
                currentPage + 1 == totalPages,
                currentPage + 1 < totalPages
        );
    }

    public ReviewResponse getReviewById(UUID id) {
        return reviewMapper.toReviewResponse(
                reviewRepository.getReviewById(id).orElseThrow(() -> new ResourceNotFoundException("Review with id " + id + " not found"))
        );
    }

}
