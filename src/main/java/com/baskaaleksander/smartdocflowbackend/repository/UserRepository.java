package com.baskaaleksander.smartdocflowbackend.repository;

import com.baskaaleksander.smartdocflowbackend.model.Role;
import com.baskaaleksander.smartdocflowbackend.model.User;
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

    @Query("select u from User u left join fetch u.roles where u.username = :username")
    Optional<User> findUserByUsernameWithRoles(String username);

    @Query("select u from User u left join fetch u.roles where u.id = :userId")
    Optional<User> findUserByIdWithRoles(UUID userId);

    @Query("update User u set u.isActive = :isActive where u.id = :userId")
    @Modifying
    void setIsActive(UUID userId, boolean isActive);

    @Query("select u.isActive from User u where u.id = :userId")
    Optional<Boolean> getUserStatusById(UUID userId);

    @Query("update User u set u.roles = :roles where u.id = :userId")
    @Modifying
    int updateUserRoles(UUID userId, Set<Role> roles);
}
