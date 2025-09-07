package com.baskaaleksander.smartdocflowbackend.dto.response;


import java.util.List;

public record UserResponse (long id, String username, List<String> roles) {}
