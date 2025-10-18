package com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model;

import java.util.Set;
import java.util.UUID;

public class Role {
    private long id;
    private String description;
    private String role;
    private Set<UUID> userIds;
}
