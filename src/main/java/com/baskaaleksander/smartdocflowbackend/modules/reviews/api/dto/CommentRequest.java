package com.baskaaleksander.smartdocflowbackend.modules.reviews.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class CommentRequest {

    @NotNull
    @Size(max = 500)
    private String comment;

}
