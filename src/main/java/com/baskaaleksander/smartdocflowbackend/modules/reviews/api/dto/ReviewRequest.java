package com.baskaaleksander.smartdocflowbackend.modules.reviews.api.dto;

import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.ReviewStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewRequest {

    @NotNull
    ReviewStatus status;

    @Size(max = 500)
    private String comment = "";
}
