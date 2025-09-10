package com.baskaaleksander.smartdocflowbackend.dto.request;

import com.baskaaleksander.smartdocflowbackend.enums.ReviewStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewRequest {

    @NotNull
    ReviewStatus status;

    @Size(max = 500)
    private String comment = "";
}
