package com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.port;

import java.util.UUID;

public interface DocumentCommandPort {
    void updateStatus(UUID documentId, String status);
}
