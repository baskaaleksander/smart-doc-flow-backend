package com.baskaaleksander.smartdocflowbackend.dto.response;


import java.util.List;
import java.util.UUID;

public record UserResponse (UUID id, String username, List<String> roles, boolean isActive) {}
