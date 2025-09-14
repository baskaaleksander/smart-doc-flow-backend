package com.baskaaleksander.smartdocflowbackend.service;

import com.baskaaleksander.smartdocflowbackend.dto.request.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.dto.request.ReviewRequest;
import com.baskaaleksander.smartdocflowbackend.dto.response.PagingResult;
import com.baskaaleksander.smartdocflowbackend.dto.response.ReviewEventResponse;
import com.baskaaleksander.smartdocflowbackend.dto.response.ReviewResponse;
import com.baskaaleksander.smartdocflowbackend.enums.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.enums.ReviewEventType;
import com.baskaaleksander.smartdocflowbackend.enums.ReviewStatus;
import com.baskaaleksander.smartdocflowbackend.exceptions.ResourceConflictException;
import com.baskaaleksander.smartdocflowbackend.exceptions.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.mapper.ReviewEventMapper;
import com.baskaaleksander.smartdocflowbackend.mapper.ReviewMapper;
import com.baskaaleksander.smartdocflowbackend.model.Review;
import com.baskaaleksander.smartdocflowbackend.model.ReviewEvent;
import com.baskaaleksander.smartdocflowbackend.model.User;
import com.baskaaleksander.smartdocflowbackend.repository.DocumentRepository;
import com.baskaaleksander.smartdocflowbackend.repository.ReviewEventRepository;
import com.baskaaleksander.smartdocflowbackend.repository.ReviewRepository;
import com.baskaaleksander.smartdocflowbackend.repository.UserRepository;
import com.baskaaleksander.smartdocflowbackend.utils.PaginationUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public ReviewService(
            ReviewRepository reviewRepository,
            UserRepository userRepository,
            ReviewMapper reviewMapper, DocumentService documentService, DocumentRepository documentRepository, ReviewEventRepository reviewEventRepository, ReviewEventMapper reviewEventMapper) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.reviewMapper = reviewMapper;
        this.documentRepository = documentRepository;
        this.reviewEventRepository = reviewEventRepository;
        this.reviewEventMapper = reviewEventMapper;
    }

    @Transactional
    public ReviewResponse handleReviewStatusChange(UUID reviewId, String username, ReviewRequest body) {
        if(body.getComment().isBlank()) {
            return switch (body.getStatus()) {
                case IN_PROGRESS -> claimReview(reviewId, username);
                case PENDING -> releaseReview(reviewId, username);
                default -> throw new ResourceConflictException("This action requires a comment");
            };
        } else {
            return switch(body.getStatus()) {
                case APPROVED -> approveDocument(reviewId, username, body.getComment());
                case REJECTED -> rejectDocument(reviewId, username, body.getComment());
                default -> throw new ResourceConflictException("Comment not allowed for this action");
            };
        }
    }

    private void logReviewEvent(User reviewer, Review review, ReviewEventType eventType, String comment) {
        ReviewEvent reviewEvent = new ReviewEvent();

        reviewEvent.setReviewer(reviewer);
        reviewEvent.setReview(review);
        reviewEvent.setEventType(eventType);
        if (comment != null) reviewEvent.setComment(comment);

        reviewEventRepository.save(reviewEvent);
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

        logReviewEvent(reviewer, review,  ReviewEventType.APPROVED, comment);


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

    public ReviewEventResponse getReviewEventById(UUID id) {
        return reviewEventMapper.toReviewEventResponse(
                reviewEventRepository.getReviewEventById(id).orElseThrow(() -> new ResourceNotFoundException("Review event with id " + id + " not found"))
        );
    }

    public PagingResult<ReviewEventResponse> getReviewEvents(UUID id, PaginationRequest request, String eventType) {
        Pageable pageable = PaginationUtil.getPageable(request);
        Page<ReviewEvent> reviewEvents;

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
