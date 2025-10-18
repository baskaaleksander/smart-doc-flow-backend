package com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model;

import com.baskaaleksander.smartdocflowbackend.modules.users.domain.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor @NoArgsConstructor
@Getter @Setter
public class ReviewEvent {
    private UUID id;
    private ReviewEventType eventType;
    private String comment;
    private ReviewerBasic reviewer;
    private UUID reviewId;
    private Instant createdAt;
}
