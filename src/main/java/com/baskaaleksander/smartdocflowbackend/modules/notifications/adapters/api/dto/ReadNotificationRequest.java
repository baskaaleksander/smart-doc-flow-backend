package com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public record ReadNotificationRequest(
        @NotNull Boolean read
) {
}
