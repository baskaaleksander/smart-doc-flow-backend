package com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.mapping;

import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.entity.RoleEntity;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.Role;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class RolePersistenceMapper {

    public Role toDomain(RoleEntity e) {
        Set<UUID> userIds = e.getUsers() != null ? e.getUsers().stream().map(UserEntity::getId).collect(Collectors.toSet()) : null;

        return new Role(
                e.getId(),
                e.getDescription(),
                e.getRole(),
                userIds
        );
    }
}
