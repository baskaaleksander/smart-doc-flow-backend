package com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.mapping;

import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.entity.RoleEntity;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.AuthUser;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class AuthUserPersistenceMapper {

    public AuthUser toDomain(UserEntity e) {
        return new AuthUser(
                e.getId(),
                e.getUsername(),
                e.getEmail(),
                e.getPassword(),
                e.getRoles() != null ? e.getRoles().stream().map(RoleEntity::getRole).collect(Collectors.toSet()) : null,
                e.isActive(),
                e.getCreatedAt()
        );
    }
}
