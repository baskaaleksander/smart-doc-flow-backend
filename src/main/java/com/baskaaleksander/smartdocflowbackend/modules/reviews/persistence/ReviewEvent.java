package com.baskaaleksander.smartdocflowbackend.modules.reviews.persistence;

import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.ReviewEventType;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.entity.UserEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@EqualsAndHashCode(of = "id")
@Table(name = "review_events")
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
    private UserEntity reviewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

}
