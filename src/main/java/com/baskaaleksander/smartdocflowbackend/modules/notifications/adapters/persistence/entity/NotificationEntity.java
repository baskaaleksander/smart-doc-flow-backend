package com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.persistence.entity;

import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.model.NotificationType;
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
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String username;
    private NotificationType type;
    private String message;
    private boolean read = false;
    private Instant createdAt = Instant.now();
}
