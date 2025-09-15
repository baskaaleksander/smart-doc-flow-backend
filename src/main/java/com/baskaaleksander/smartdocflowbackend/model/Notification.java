package com.baskaaleksander.smartdocflowbackend.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID userId;
    private String type;
    private String message;
    private boolean read = false;
    private Instant createdAt = Instant.now();
}
