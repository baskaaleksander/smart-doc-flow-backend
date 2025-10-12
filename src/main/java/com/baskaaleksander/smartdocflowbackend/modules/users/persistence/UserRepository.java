package com.baskaaleksander.smartdocflowbackend.modules.users.persistence;

import com.baskaaleksander.smartdocflowbackend.modules.auth.persistence.Role;
import com.baskaaleksander.smartdocflowbackend.modules.users.domain.UserRoleCount;
import com.baskaaleksander.smartdocflowbackend.modules.users.domain.UserStatusCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    @Query("select u from User u left join fetch u.roles where u.username = :username")
    Optional<User> findUserByUsernameWithRoles(String username);

    @Query("select u from User u left join fetch u.roles where u.id = :userId")
    Optional<User> findUserByIdWithRoles(UUID userId);

    @Query("update User u set u.active = :isActive where u.id = :userId")
    @Modifying
    void setIsActive(UUID userId, boolean isActive);

    @Query("select u.active from User u where u.id = :userId")
    Optional<Boolean> getUserStatusById(UUID userId);

    @Query("update User u set u.roles = :roles where u.id = :userId")
    @Modifying
    int updateUserRoles(UUID userId, Set<Role> roles);

    @Query("""
    SELECT r.role AS role, COUNT(DISTINCT u) AS count
    FROM Role r
    LEFT JOIN r.users u
    GROUP BY r.role
    """)
    List<UserRoleCount> countUsersPerRole();

    @Query("select u.active as active, count(u) as count from User u group by u.active")
    List<UserStatusCount> countUsersPerStatus();

    Page<User> findAll(Pageable pageable);
}
