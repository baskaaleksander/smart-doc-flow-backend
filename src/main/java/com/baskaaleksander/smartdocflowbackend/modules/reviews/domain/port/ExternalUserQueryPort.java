package com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.port;

import java.util.Optional;
import java.util.UUID;

public interface ExternalUserQueryPort {
    Optional<String> findByUsername(String username);

    Optional<String> findById(UUID id);

    Optional<UUID> findIdByUsername(String username);
}
