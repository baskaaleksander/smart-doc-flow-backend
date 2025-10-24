package com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.mapping;

import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.entity.RoleEntity;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.Role;
import org.springframework.stereotype.Component;

@Component
public class RolePersistenceMapper {

    public Role toDomain(RoleEntity e) {

        return new Role(
                e.getId(),
                e.getDescription(),
                e.getRole()
        );
    }
}
