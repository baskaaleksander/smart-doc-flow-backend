package com.baskaaleksander.smartdocflowbackend.modules.users.domain.port;

import com.baskaaleksander.smartdocflowbackend.modules.users.domain.model.User;

import java.util.UUID;

public interface UserCommandPort {
    void setIsActive(UUID userId, boolean isActive);
    User save(User user);
}
