package com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model;

import java.util.UUID;

public record ReviewerBasic(
        UUID id,
        String name
) {
}
