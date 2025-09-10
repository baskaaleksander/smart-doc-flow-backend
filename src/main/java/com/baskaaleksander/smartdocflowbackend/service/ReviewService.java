package com.baskaaleksander.smartdocflowbackend.service;

import com.baskaaleksander.smartdocflowbackend.dto.response.ReviewResponse;
import com.baskaaleksander.smartdocflowbackend.enums.DocumentStatus;
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

        return reviewMapper.toReviewResponse(review);
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

        return reviewMapper.toReviewResponse(review);

    }

    public List<ReviewResponse> getAllReviews(Optional<String> status) {

        if (status.isPresent()) {
            String statusVal = status.get();
            return reviewRepository.getReviewsByStatus(ReviewStatus.valueOf(statusVal)).stream().map(reviewMapper::toReviewResponse).toList();
        }
        return reviewRepository.findAll().stream().map(reviewMapper::toReviewResponse).toList();
    }

    public ReviewResponse getReviewById(long id) {
        return reviewMapper.toReviewResponse(
                reviewRepository.getReviewById(id).orElseThrow(() -> new ResourceNotFoundException("Review with id " + id + " not found"))
        );
    }

}
