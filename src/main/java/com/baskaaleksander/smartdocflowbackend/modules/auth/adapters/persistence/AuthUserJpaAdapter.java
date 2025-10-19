package com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.entity.RoleEntity;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.mapping.AuthUserPersistenceMapper;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.spring.SpringDataRoleRepository;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.AuthUser;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port.AuthUserCommandPort;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port.AuthUserQueryPort;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.entity.UserEntity;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.spring.SpringDataUserRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class AuthUserJpaAdapter implements AuthUserQueryPort, AuthUserCommandPort {

    private final SpringDataUserRepository userRepo;
    private final SpringDataRoleRepository roleRepo;
    private final AuthUserPersistenceMapper mapper;

    public AuthUserJpaAdapter(
            SpringDataUserRepository userRepo,
            SpringDataRoleRepository roleRepo,
            AuthUserPersistenceMapper mapper
    ) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.mapper = mapper;
    }
    @Override
    @Transactional
    public AuthUser save(AuthUser user) {
        UserEntity entity = (user.getId() != null) ? userRepo.findById(user.getId())
                .orElseGet(UserEntity::new) : new UserEntity();

        entity.setUsername(user.getUsername());
        entity.setEmail(user.getEmail());
        entity.setActive(user.isActive());

        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            entity.setPassword(user.getPassword());
        }

        Set<String> requestedRoles = Optional.ofNullable(user.getRoles()).orElse(Set.of());
        Set<RoleEntity> roleEntities = requestedRoles.isEmpty()
                ? Set.of()
                : roleRepo.findAllByRoleIn(requestedRoles);

        if (roleEntities.size() != requestedRoles.size()) {
            throw new ResourceNotFoundException("One or more roles not found");
        }

        entity.setRoles(roleEntities);

        UserEntity saved = userRepo.save(entity);

        return mapper.toDomain(saved);
    }

    @Override
    public Optional<AuthUser> findUserByUsernameWithRoles(String username) {
        return userRepo.findUserByUsernameWithRoles(username).map(mapper::toDomain);
    }

    @Override
    public Optional<AuthUser> findByIdWithRoles(UUID id) {
        return userRepo.findUserByIdWithRoles(id).map(mapper::toDomain);
    }

    @Override
    public Optional<AuthUser> findById(UUID id) {
        return userRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<AuthUser> findByEmail(String email) {
        return userRepo.findByEmail(email).map(mapper::toDomain);
    }
}
