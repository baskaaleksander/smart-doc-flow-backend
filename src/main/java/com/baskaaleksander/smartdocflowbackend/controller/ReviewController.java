package com.baskaaleksander.smartdocflowbackend.controller;

import com.baskaaleksander.smartdocflowbackend.dto.request.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.dto.request.ReviewRequest;
import com.baskaaleksander.smartdocflowbackend.dto.response.PagingResult;
import com.baskaaleksander.smartdocflowbackend.dto.response.ReviewEventResponse;
import com.baskaaleksander.smartdocflowbackend.dto.response.ReviewResponse;
import com.baskaaleksander.smartdocflowbackend.model.Review;
import com.baskaaleksander.smartdocflowbackend.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.data.domain.Sort;
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
    public ResponseEntity<PagingResult<ReviewResponse>> getAllReviews(
            @RequestParam("status") Optional<String> status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "id") String sortField,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        PaginationRequest request = new PaginationRequest(page, size, sortField, direction);

        return new ResponseEntity<>(reviewService.getAllReviews(status, request), HttpStatus.OK);
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<ReviewEventResponse> getReviewEvent(@PathVariable("eventId") UUID eventId) {
        return new ResponseEntity<>(reviewService.getReviewEventById(eventId), HttpStatus.OK);
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
    public ResponseEntity<List<ReviewEventResponse>> getAllEventsForReview(@PathVariable("reviewId") UUID reviewId) {
        return new ResponseEntity<>(reviewService.getReviewEvents(reviewId), HttpStatus.OK);
    }

}
