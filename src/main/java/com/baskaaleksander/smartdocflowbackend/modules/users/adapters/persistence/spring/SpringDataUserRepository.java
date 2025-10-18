package com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.spring;

import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.entity.UserEntity;
import com.baskaaleksander.smartdocflowbackend.modules.users.domain.view.UserRoleCount;
import com.baskaaleksander.smartdocflowbackend.modules.users.domain.view.UserStatusCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataUserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findById(UUID userId);
    @Query("select u from UserEntity u left join fetch u.roles where u.username = :username")
    Optional<UserEntity> findUserByUsernameWithRoles(String username);

    @Query("select u from UserEntity u left join fetch u.roles where u.id = :userId")
    Optional<UserEntity> findUserByIdWithRoles(UUID userId);

    @Query("update UserEntity u set u.active = :isActive where u.id = :userId")
    @Modifying
    void setIsActive(UUID userId, boolean isActive);

    @Query("select u.active from UserEntity u where u.id = :userId")
    Optional<Boolean> getUserStatusById(UUID userId);

    @Query("""
    SELECT r.role AS role, COUNT(DISTINCT u) AS count
    FROM Role r
    LEFT JOIN r.users u
    GROUP BY r.role
    """)
    List<UserRoleCount> countUsersPerRole();

    @Query("select u.active as active, count(u) as count from UserEntity u group by u.active")
    List<UserStatusCount> countUsersPerStatus();

    Page<UserEntity> findAll(Pageable pageable);
}
