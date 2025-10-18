package com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.port;

import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewStatus;

import java.util.UUID;

public interface ReviewCommandPort {
    void updateStatus(UUID reviewId, ReviewStatus status);
}
