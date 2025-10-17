package com.baskaaleksander.smartdocflowbackend.modules.users.domain.port;

import java.util.Set;

public interface RoleQueryPort {
    Set<String> findAllByRoleIn(Set<String> roles);
}
