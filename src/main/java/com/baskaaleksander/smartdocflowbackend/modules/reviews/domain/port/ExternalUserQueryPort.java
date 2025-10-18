package com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.port;

import java.util.Optional;
import java.util.UUID;

public interface ExternalUserQueryPort {
    Optional<UUID> findByUsername(String username);
}
