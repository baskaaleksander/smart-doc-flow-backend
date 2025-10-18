package com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model;

import com.baskaaleksander.smartdocflowbackend.modules.users.domain.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor @NoArgsConstructor
@Getter @Setter
public class Review {
    private UUID id;
    private UUID documentIds;
    private ReviewStatus status;
    private UUID reviewerId;
    private List<ReviewEvent> reviewEvents;
    private String comment;
    private Instant createdAt;
    private Instant updatedAt;
    private int version;
}
