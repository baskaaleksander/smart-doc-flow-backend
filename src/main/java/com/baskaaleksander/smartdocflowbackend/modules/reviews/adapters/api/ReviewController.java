package com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.api;

import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.api.dto.CommentRequest;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.api.dto.ReviewEventResponse;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.api.dto.ReviewRequest;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.api.dto.ReviewResponse;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.application.ReviewApplicationService;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.application.ReviewEventApplicationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/reviews")
@PreAuthorize("hasAnyRole('ADMIN', 'REVIEW')")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Endpoints for managing reviews and review events")
public class ReviewController {

    private final ReviewApplicationService reviewService;
    private final ReviewEventApplicationService reviewEventService;

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
        return new ResponseEntity<>(reviewEventService.getReviewEventById(eventId), HttpStatus.OK);
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

    @PostMapping("/{reviewId}/comment")
    public ResponseEntity<ReviewEventResponse> commentReview(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody @Valid CommentRequest body,
            @PathVariable("reviewId") UUID reviewId
    ) {
        return new ResponseEntity<>(reviewService.commentReview(user.getUsername(), body.comment(), reviewId), HttpStatus.OK);
    }

    @GetMapping("/{reviewId}/events")
    public ResponseEntity<PagingResult<ReviewEventResponse>> getAllEventsForReview(
            @PathVariable("reviewId") UUID reviewId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "id") String sortField,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @RequestParam(defaultValue = "ALL") String eventType
    ) {

        PaginationRequest request = new PaginationRequest(page, size, sortField, direction);
        return new ResponseEntity<>(reviewEventService.getReviewEvents(reviewId, request, eventType), HttpStatus.OK);
    }

}
