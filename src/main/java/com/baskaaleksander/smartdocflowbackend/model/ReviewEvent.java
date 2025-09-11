package com.baskaaleksander.smartdocflowbackend.model;

import com.baskaaleksander.smartdocflowbackend.enums.ReviewEventType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
public class ReviewEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private ReviewEventType eventType;

    @Column(nullable = true)
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

}
