package com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.persistence.entity;

import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.model.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

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

    @Column(nullable = false)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;
    @Column(nullable = false)
    private String message;
    @Column(nullable = false)
    private boolean read = false;
    @Column(nullable = false)
    @CreationTimestamp
    private Instant createdAt = Instant.now();
}
