package com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence;

import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.entity.RoleEntity;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.entity.DocumentEntity;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.entity.UserEntity;
import com.baskaaleksander.smartdocflowbackend.modules.users.domain.model.User;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class UserPersistenceMapper {

    public User toDomain(UserEntity e) {
        return new User(
                e.getId(),
                e.getUsername(),
                e.getEmail(),
                e.getPassword(),
                e.getRoles().stream().map(RoleEntity::getRole).collect(Collectors.toSet()),
                e.isActive(),
                e.getDocuments().stream().map(DocumentEntity::getId).collect(Collectors.toSet()),
                e.getCreatedAt()
        );
    }
}
