package com.baskaaleksander.smartdocflowbackend.controller;

import com.baskaaleksander.smartdocflowbackend.dto.request.ReviewRequest;
import com.baskaaleksander.smartdocflowbackend.dto.response.ReviewEventResponse;
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
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/reviews")
@PreAuthorize("hasAnyRole('ADMIN', 'REVIEW')")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(
            ReviewService reviewService
    ) {
        this.reviewService = reviewService;
    }

    @GetMapping("")
    public ResponseEntity<List<ReviewResponse>> getAllReviews(@RequestParam("status") Optional<String> status) {
        return new ResponseEntity<>(reviewService.getAllReviews(status), HttpStatus.OK);
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<ReviewEventResponse> getReviewEvent() {
        return null;
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> getReviewById(@PathVariable("reviewId") UUID reviewId) {
        return new ResponseEntity<>(reviewService.getReviewById(reviewId), HttpStatus.OK);
    }

    @PatchMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> changeReviewStatus(
            @RequestBody @Valid ReviewRequest body,
            @PathVariable("reviewId") UUID reviewId,
            @AuthenticationPrincipal UserDetails user
            ) {

        return new ResponseEntity<>(reviewService.handleReviewStatusChange(reviewId, user.getUsername(), body), HttpStatus.OK);
    }

    @GetMapping("/{reviewId}/events")
    public ResponseEntity<List<ReviewEventResponse>> getAllEventsForReview() {
        return null;
    }

}
