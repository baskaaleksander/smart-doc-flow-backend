package com.baskaaleksander.smartdocflowbackend.modules.users.domain.port;

import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.users.domain.model.User;
import com.baskaaleksander.smartdocflowbackend.modules.users.domain.view.UserRoleCount;
import com.baskaaleksander.smartdocflowbackend.modules.users.domain.view.UserStatusCount;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserQueryPort {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findById(UUID userId);
    Optional<User> findUserByIdWithRoles(UUID userId);
    Optional<Boolean> getUserStatusById(UUID userId);
    List<UserRoleCount> countUsersPerRole();
    List<UserStatusCount> countUsersPerStatus();
    PagingResult<User> findAll(PaginationRequest request);
}
