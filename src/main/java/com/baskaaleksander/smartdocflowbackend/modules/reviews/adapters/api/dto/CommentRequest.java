package com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public record CommentRequest(
        @NotNull @Size(max = 500) String comment
) {
}
