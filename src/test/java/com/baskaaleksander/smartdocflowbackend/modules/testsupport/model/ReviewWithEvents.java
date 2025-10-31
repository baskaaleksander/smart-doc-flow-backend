package com.baskaaleksander.smartdocflowbackend.modules.testsupport.model;

import java.util.UUID;

public record ReviewWithEvents(UUID reviewId, UUID commentEventId) {
}
