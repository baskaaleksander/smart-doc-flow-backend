package com.baskaaleksander.smartdocflowbackend.modules.auth.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    @Query("select r from Role r where r.role = :role")
    Optional<Role> findRoleByRole(String role);

    @Query("select r from Role r where r.role in :roles")
    Set<Role> findAllByRoleIn(Set<String> roles);
}
