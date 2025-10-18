package com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api.dto;


import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserResponse (UUID id, String username, String email, List<String> roles, boolean active, Instant createdAt) {}
