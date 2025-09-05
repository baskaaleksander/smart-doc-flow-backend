package com.baskaaleksander.smartdocflowbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {

    private int status;
    private String message;
    private Object details;
    private String path;
    private LocalDateTime timestamp;
}
