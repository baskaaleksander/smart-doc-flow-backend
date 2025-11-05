package com.baskaaleksander.smartdocflowbackend.modules.reviews.application.security;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.security.CustomUserDetails;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.port.ReviewDocumentQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component("reviewAccess")
@RequiredArgsConstructor
public class ReviewAccessEvaluation {

    private final ReviewDocumentQueryPort documentRepo;

    public boolean canViewEvents(Authentication authentication, UUID reviewId) {

        List<String> roles = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

        if (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_REVIEW")) return true;

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        UUID ownerId = documentRepo.getOwnerIdByReviewId(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found"));

        return ownerId.equals(userDetails.getId());
    }
}
