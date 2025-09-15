package com.baskaaleksander.smartdocflowbackend.model;

import com.baskaaleksander.smartdocflowbackend.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String username;
    private NotificationType type;
    private String message;
    private boolean read = false;
    private Instant createdAt = Instant.now();
}
