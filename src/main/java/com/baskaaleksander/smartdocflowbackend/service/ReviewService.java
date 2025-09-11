package com.baskaaleksander.smartdocflowbackend.service;

import com.baskaaleksander.smartdocflowbackend.dto.request.ReviewRequest;
import com.baskaaleksander.smartdocflowbackend.dto.response.ReviewResponse;
import com.baskaaleksander.smartdocflowbackend.enums.ReviewStatus;
import com.baskaaleksander.smartdocflowbackend.exceptions.ResourceConflictException;
import com.baskaaleksander.smartdocflowbackend.exceptions.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.mapper.ReviewMapper;
import com.baskaaleksander.smartdocflowbackend.model.Review;
import com.baskaaleksander.smartdocflowbackend.model.User;
import com.baskaaleksander.smartdocflowbackend.repository.ReviewRepository;
import com.baskaaleksander.smartdocflowbackend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public ReviewService(
            ReviewRepository reviewRepository,
            UserRepository userRepository,
            ReviewMapper reviewMapper) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.reviewMapper = reviewMapper;
    }

    @Transactional
    public ReviewResponse handleReviewStatusChange(UUID reviewId, String username, ReviewRequest body) {
        System.out.println(body.getStatus() );
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

        review = reviewRepository.save(review);

        return reviewMapper.toReviewResponse(review);
    }

    @Transactional
    public ReviewResponse releaseReview(UUID reviewId, String username) {
        Review review = reviewRepository.getReviewById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review with ID " + reviewId + " not found"));

        if(review.getStatus() != ReviewStatus.IN_PROGRESS) {
            throw new ResourceConflictException("Review with ID " + reviewId + " cannot be released");
        }

        if(!review.getReviewer().getUsername().equalsIgnoreCase(username)) {
            throw new AccessDeniedException("You are not allowed to release this review");
        }

        review.setReviewer(null);

        reviewRepository.updateStatus(review.getId(), ReviewStatus.PENDING);

        review = reviewRepository.save(review);

        return reviewMapper.toReviewResponse(review);

    }

    @Transactional
    public ReviewResponse approveDocument(UUID reviewId, String username, String comment) {
        Review review = reviewRepository.getReviewById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review with ID " + reviewId + " not found"));

        if(review.getStatus() != ReviewStatus.IN_PROGRESS) {
            throw new ResourceConflictException("Review with ID " + reviewId + " cannot be approved, it needs to be IN_PROGRESS status");
        }

        if(!review.getReviewer().getUsername().equalsIgnoreCase(username)) {
            throw new AccessDeniedException("You are not allowed to approve this document");
        }

        review.setComment(comment);
        reviewRepository.updateStatus(reviewId, ReviewStatus.APPROVED);

        review = reviewRepository.save(review);

        return reviewMapper.toReviewResponse(review);
    }

    @Transactional
    public ReviewResponse rejectDocument(UUID reviewId, String username, String comment) {
        Review review = reviewRepository.getReviewById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review with ID " + reviewId + " not found"));

        if(review.getStatus() != ReviewStatus.IN_PROGRESS) {
            throw new ResourceConflictException("Review with ID " + reviewId + " cannot be approved, it needs to be IN_PROGRESS status");
        }

        if(!review.getReviewer().getUsername().equalsIgnoreCase(username)) {
            throw new AccessDeniedException("You are not allowed to reject this document");
        }

        review.setComment(comment);
        reviewRepository.updateStatus(reviewId, ReviewStatus.REJECTED);

        review = reviewRepository.save(review);

        return reviewMapper.toReviewResponse(review);
    }

    public List<ReviewResponse> getAllReviews(Optional<String> status) {

        if (status.isPresent()) {
            String statusVal = status.get();
            return reviewRepository.getReviewsByStatus(ReviewStatus.valueOf(statusVal)).stream().map(reviewMapper::toReviewResponse).toList();
        }
        return reviewRepository.findAll().stream().map(reviewMapper::toReviewResponse).toList();
    }

    public ReviewResponse getReviewById(UUID id) {
        return reviewMapper.toReviewResponse(
                reviewRepository.getReviewById(id).orElseThrow(() -> new ResourceNotFoundException("Review with id " + id + " not found"))
        );
    }

}
