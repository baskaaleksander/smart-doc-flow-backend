package com.baskaaleksander.smartdocflowbackend.dto.response;


import java.time.LocalDateTime;

public record ErrorResponse (int status, String message, Object details, String path, LocalDateTime timestamp) {

}