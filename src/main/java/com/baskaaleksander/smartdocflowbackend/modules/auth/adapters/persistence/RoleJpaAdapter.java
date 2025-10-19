package com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence;

import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.Role;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port.RoleCommandPort;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port.RoleQueryPort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Repository
@Transactional(readOnly = true)
public class RoleJpaAdapter implements RoleQueryPort, RoleCommandPort {
    @Override
    public Role save(Role role) {
        return null;
    }

    @Override
    public Set<Role> findAllByRoleIn(Set<String> roles) {
        return Set.of();
    }
}
