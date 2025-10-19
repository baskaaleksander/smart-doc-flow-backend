package com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port;

import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.Role;

public interface RoleCommandPort {
    Role save(Role role);
}
