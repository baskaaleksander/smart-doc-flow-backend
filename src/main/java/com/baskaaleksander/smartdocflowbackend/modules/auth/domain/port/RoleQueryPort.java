package com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port;

import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.Role;

import java.util.Optional;
import java.util.Set;

public interface RoleQueryPort {
    Set<Role> findAllByRoleIn(Set<String> roles);

    Optional<Role> findByName(String roleName);
}
