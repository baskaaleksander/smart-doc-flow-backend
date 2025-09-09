package com.baskaaleksander.smartdocflowbackend.controller;

import com.baskaaleksander.smartdocflowbackend.dto.response.ReviewResponse;
import com.baskaaleksander.smartdocflowbackend.model.Review;
import com.baskaaleksander.smartdocflowbackend.service.ReviewService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/{documentId}/claim")
    public ReviewResponse claimReview(@PathVariable("documentId") UUID documentId, @AuthenticationPrincipal UserDetails user) {
        return reviewService.claimReview(documentId, user.getUsername());
    }

    @PostMapping("/{documentId}/release")
    public ReviewResponse releaseReview(@PathVariable("documentId") UUID documentId, @AuthenticationPrincipal UserDetails user) {
        return reviewService.releaseReview(documentId, user.getUsername());
    }
}
