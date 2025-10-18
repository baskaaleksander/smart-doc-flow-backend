package com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.spring;

import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface SpringDataRoleRepository extends JpaRepository<RoleEntity, Long> {

    @Query("select r from RoleEntity r where r.role = :role")
    Optional<RoleEntity> findRoleByRole(String role);

    @Query("select r from RoleEntity r where r.role in :roles")
    Set<RoleEntity> findAllByRoleIn(Set<String> roles);
}
