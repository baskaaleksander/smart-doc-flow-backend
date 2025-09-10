package com.baskaaleksander.smartdocflowbackend.dto.request;

import com.baskaaleksander.smartdocflowbackend.enums.ReviewStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewRequest {

    @NotBlank
    String status;

    @Size(max = 500)
    private String comment = "";
}
