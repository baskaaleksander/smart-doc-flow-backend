package com.baskaaleksander.smartdocflowbackend.service;

import com.baskaaleksander.smartdocflowbackend.dto.response.ReviewResponse;
import com.baskaaleksander.smartdocflowbackend.enums.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.enums.ReviewStatus;
import com.baskaaleksander.smartdocflowbackend.exceptions.ResourceConflictException;
import com.baskaaleksander.smartdocflowbackend.exceptions.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.model.Review;
import com.baskaaleksander.smartdocflowbackend.model.User;
import com.baskaaleksander.smartdocflowbackend.repository.ReviewRepository;
import com.baskaaleksander.smartdocflowbackend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    @Autowired
    public ReviewService(
            ReviewRepository reviewRepository,
            UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ReviewResponse claimReview(UUID documentId, String username) {
        Review review = reviewRepository.getReviewByDocId(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Review with document ID " + documentId + " not found"));

        if(review.getStatus() == ReviewStatus.IN_PROGRESS) {
            throw new ResourceConflictException("Review with document ID " + documentId + " is already claimed");
        }

        User reviewer = userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User with username " + username + " not found"));
        review.setReviewer(reviewer);

        reviewRepository.updateStatus(review.getId(), ReviewStatus.IN_PROGRESS);

        review = reviewRepository.save(review);

        return new ReviewResponse(
                review.getId(),
                review.getDocument().getId(),
                review.getStatus(),
                review.getReviewer().getId(),
                review.getCreatedAt(),
                review.getUpdatedAt(),
                review.getVersion()
        );
    }

    @Transactional
    public ReviewResponse releaseReview(UUID documentId, String username) {
        Review review = reviewRepository.getReviewByDocId(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Review with document ID " + documentId + " not found"));

        if(review.getStatus() != ReviewStatus.IN_PROGRESS) {
            throw new ResourceConflictException("Review with document ID " + documentId + " cannot be released");
        }

        if(!review.getReviewer().getUsername().equalsIgnoreCase(username)) {
            System.out.println("not allowed");
        }

        review.setReviewer(null);

        reviewRepository.updateStatus(review.getId(), ReviewStatus.PENDING);

        review = reviewRepository.save(review);

        return new ReviewResponse(
                review.getId(),
                review.getDocument().getId(),
                review.getStatus(),
                null,
                review.getCreatedAt(),
                review.getUpdatedAt(),
                review.getVersion()
        );

    }
}
