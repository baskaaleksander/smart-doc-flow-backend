package com.baskaaleksander.smartdocflowbackend.controller;

import com.baskaaleksander.smartdocflowbackend.dto.request.ReviewRequest;
import com.baskaaleksander.smartdocflowbackend.dto.response.ReviewResponse;
import com.baskaaleksander.smartdocflowbackend.model.Review;
import com.baskaaleksander.smartdocflowbackend.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/reviews")
//@PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(
            ReviewService reviewService
    ) {
        this.reviewService = reviewService;
    }

    @GetMapping("/")
    public ResponseEntity<List<ReviewResponse>> getAllReviews() {
        return null;
    }

    @GetMapping("/queue")
    public ResponseEntity<List<ReviewResponse>> getReviewsQueue(@RequestParam(defaultValue = "PENDING") String status) {
        return null;
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> getReviewById(@PathVariable("reviewId") long reviewId) {
        return null;
    }

    @PostMapping("/{documentId}/claim")
    public ResponseEntity<ReviewResponse> claimReview(@PathVariable("documentId") UUID documentId, @AuthenticationPrincipal UserDetails user) {
        return new ResponseEntity<>(reviewService.claimReview(documentId, user.getUsername()), HttpStatus.OK);
    }

    @PostMapping("/{documentId}/release")
    public ResponseEntity<ReviewResponse> releaseReview(@PathVariable("documentId") UUID documentId, @AuthenticationPrincipal UserDetails user) {
        return new ResponseEntity<>(reviewService.releaseReview(documentId, user.getUsername()), HttpStatus.OK);
    }

    @PostMapping("/{documentId}/approve")
    public ResponseEntity<ReviewResponse> approveDocument(
            @PathVariable("documentId") UUID documentId,
            @AuthenticationPrincipal UserDetails user
            ) {
        return null;
    }

    @PostMapping("/{documentId}/reject")
    public ResponseEntity<ReviewResponse> rejectDocument(
            @PathVariable("documentId") UUID documentId,
            @AuthenticationPrincipal UserDetails user,
            @Valid ReviewRequest review
    ) {
        return null;
    }
}
