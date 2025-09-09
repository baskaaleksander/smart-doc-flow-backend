package com.baskaaleksander.smartdocflowbackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewRequest {

    @NotBlank @Size(min = 10)
    private String comment;
}
