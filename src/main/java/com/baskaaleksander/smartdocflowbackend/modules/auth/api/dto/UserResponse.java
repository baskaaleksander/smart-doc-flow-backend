package com.baskaaleksander.smartdocflowbackend.modules.auth.api.dto;


import java.util.List;
import java.util.UUID;

public record UserResponse (UUID id, String username, List<String> roles, boolean active) {}
