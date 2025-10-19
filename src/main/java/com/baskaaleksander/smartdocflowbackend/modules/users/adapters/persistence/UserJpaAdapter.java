package com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationUtil;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.entity.RoleEntity;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.spring.SpringDataRoleRepository;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.entity.DocumentEntity;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.spring.SpringDataDocumentRepository;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.entity.UserEntity;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.spring.SpringDataUserRepository;
import com.baskaaleksander.smartdocflowbackend.modules.users.domain.model.User;
import com.baskaaleksander.smartdocflowbackend.modules.users.domain.port.UserRoleQueryPort;
import com.baskaaleksander.smartdocflowbackend.modules.users.domain.port.UserCommandPort;
import com.baskaaleksander.smartdocflowbackend.modules.users.domain.port.UserQueryPort;
import com.baskaaleksander.smartdocflowbackend.modules.users.domain.view.UserRoleCount;
import com.baskaaleksander.smartdocflowbackend.modules.users.domain.view.UserStatusCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@Transactional(readOnly = true)
public class UserJpaAdapter implements UserQueryPort, UserCommandPort, UserRoleQueryPort {

    private final SpringDataUserRepository userRepo;
    private final SpringDataRoleRepository roleRepo;
    private final SpringDataDocumentRepository documentRepo;
    private final UserPersistenceMapper mapper;

    public UserJpaAdapter(
            SpringDataUserRepository userRepo,
            SpringDataRoleRepository roleRepo,
            SpringDataDocumentRepository documentRepo,
            UserPersistenceMapper mapper
    ) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.documentRepo = documentRepo;
        this.mapper = mapper;
    }

    @Override
    public Set<String> findAllByRoleIn(Set<String> roles) {
        if (roles == null || roles.isEmpty()) return Set.of();

        return roleRepo.findAllByRoleIn(roles).stream().map(RoleEntity::getRole).collect(Collectors.toSet());
    }

    @Override
    public void setIsActive(UUID userId, boolean isActive) {
        userRepo.setIsActive(userId, isActive);
    }

    @Override
    public User save(User user) {
        UserEntity entity = userRepo.findById(user.getId())
                .orElseGet(UserEntity::new);

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

        entity.getRoles().clear();
        entity.getRoles().addAll(roleEntities);

        Set<UUID> requestedDocIds = Optional.ofNullable(user.getDocumentIds()).orElse(Set.of());

        Set<DocumentEntity> docs = requestedDocIds.isEmpty()
                ? Set.of()
                : documentRepo.findAllByIdIn(requestedDocIds);

        if (docs.size() != requestedDocIds.size()) {
            throw new ResourceNotFoundException("One or more documents not found");
        }

        entity.getDocuments().clear();
        entity.getDocuments().addAll(docs);

        UserEntity saved = userRepo.save(entity);

        return mapper.toDomain(saved);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepo.findByUsername(username).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepo.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findById(UUID userId) {
        return userRepo.findById(userId).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findUserByIdWithRoles(UUID userId) {
        return userRepo.findUserByIdWithRoles(userId).map(mapper::toDomain);
    }

    @Override
    public Optional<Boolean> getUserStatusById(UUID userId) {
        return userRepo.getUserStatusById(userId);
    }

    @Override
    public List<UserRoleCount> countUsersPerRole() {
        return userRepo.countUsersPerRole();
    }

    @Override
    public List<UserStatusCount> countUsersPerStatus() {
        return userRepo.countUsersPerStatus();
    }

    @Override
    public PagingResult<User> findAll(PaginationRequest request) {
        Pageable pageable = PaginationUtil.getPageable(request);
        Page<UserEntity> page = userRepo.findAll(pageable);

        List<User> content = page.getContent().stream().map(mapper::toDomain).toList();

        return new PagingResult<>(
                content,
                page.getTotalPages(),
                page.getTotalElements(),
                page.getSize(),
                page.getNumber(),
                page.isLast(),
                page.isEmpty()
        );
    }
}
