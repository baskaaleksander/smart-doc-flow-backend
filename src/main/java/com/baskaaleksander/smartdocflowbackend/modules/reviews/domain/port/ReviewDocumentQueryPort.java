package com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.port;

import java.util.Optional;
import java.util.UUID;

public interface ReviewDocumentQueryPort {
    Optional<UUID> getOwnerIdByReviewId(UUID reviewId);
}
