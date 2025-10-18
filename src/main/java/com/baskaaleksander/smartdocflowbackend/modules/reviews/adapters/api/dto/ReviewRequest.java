package com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.api.dto;

import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public record ReviewRequest(
        @NotNull ReviewStatus status,
        @Size(max = 500) String comment
) {
}
