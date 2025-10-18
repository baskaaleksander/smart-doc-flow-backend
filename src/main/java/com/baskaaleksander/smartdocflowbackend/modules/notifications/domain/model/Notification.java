package com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@NoArgsConstructor @AllArgsConstructor
@Getter @Setter
public class Notification {
    private UUID id;
    private String username;
    private NotificationType type;
    private String message;
    private boolean read;
    private Instant createdAt;
}
