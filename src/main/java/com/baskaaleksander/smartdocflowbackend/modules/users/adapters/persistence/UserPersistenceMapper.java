package com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence;

import com.baskaaleksander.smartdocflowbackend.modules.auth.persistence.Role;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.Document;
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
                e.getRoles().stream().map(Role::getRole).collect(Collectors.toSet()),
                e.isActive(),
                e.getDocuments().stream().map(Document::getId).collect(Collectors.toSet()),
                e.getCreatedAt()
        );
    }
}
