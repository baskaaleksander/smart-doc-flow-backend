package com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence;

import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.entity.RoleEntity;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.mapping.RolePersistenceMapper;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.spring.SpringDataRoleRepository;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.Role;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port.RoleCommandPort;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port.RoleQueryPort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Repository
@Transactional(readOnly = true)
public class RoleJpaAdapter implements RoleQueryPort, RoleCommandPort {

    private final SpringDataRoleRepository roleRepo;
    private final RolePersistenceMapper mapper;

    public RoleJpaAdapter(
            SpringDataRoleRepository roleRepo,
            RolePersistenceMapper mapper
            ) {
        this.roleRepo = roleRepo;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Role save(Role role) {
        RoleEntity entity = roleRepo.findById(role.getId()).orElseGet(RoleEntity::new);

        entity.setId(role.getId());
        entity.setDescription(role.getDescription());
        entity.setRole(role.getRole());

        RoleEntity saved = roleRepo.save(entity);

        return mapper.toDomain(saved);
    }

    @Override
    public Set<Role> findAllByRoleIn(Set<String> roles) {
        return roleRepo.findAllByRoleIn(roles).stream().map(mapper::toDomain).collect(Collectors.toSet());
    }
}
