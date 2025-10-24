package com.baskaaleksander.smartdocflowbackend.modules.users.application;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceConflictException;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api.dto.UserResponse;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.api.UserApiMapper;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.api.dto.EditUserAccountAdminRequest;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.api.dto.EditUserAccountRequest;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.api.dto.UserStatsResponse;
import com.baskaaleksander.smartdocflowbackend.modules.users.domain.model.User;
import com.baskaaleksander.smartdocflowbackend.modules.users.domain.port.UserCommandPort;
import com.baskaaleksander.smartdocflowbackend.modules.users.domain.port.UserQueryPort;
import com.baskaaleksander.smartdocflowbackend.modules.users.domain.port.UserRoleQueryPort;
import com.baskaaleksander.smartdocflowbackend.modules.users.domain.view.UserRoleCount;
import com.baskaaleksander.smartdocflowbackend.modules.users.domain.view.UserStatusCount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class UserApplicationService {

    private final UserQueryPort userQueryPort;
    private final UserCommandPort userCommandPort;
    private final UserRoleQueryPort roleQueryPort;
    private final UserApiMapper mapper;

    @Transactional(readOnly = true)
    public PagingResult<UserResponse> getAllUsers(PaginationRequest request) {

        PagingResult<User> userPagingResult = userQueryPort.findAll(request);

        List<UserResponse> content = userPagingResult.content().stream().map(mapper::toUserResponse).toList();

        return new PagingResult<>(
                content,
                userPagingResult.totalPages(),
                userPagingResult.totalElements(),
                userPagingResult.size(),
                userPagingResult.page(),
                userPagingResult.last(),
                userPagingResult.next()
        );
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(String userId) {
        User user = userQueryPort.findUserByIdWithRoles(UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return mapper.toUserResponse(user);
    }

    @Transactional
    public String inactivateUser(String userId) {

        UUID userUUID = UUID.fromString(userId);
        boolean status = userQueryPort.getUserStatusById(userUUID).orElseThrow(() -> new ResourceNotFoundException("User with ID " + userId + " not found"));

        if (!status) {
            throw new ResourceConflictException("User is already inactive");
        }

        userCommandPort.setIsActive(userUUID, false);

        return "User inactivated";
    }

    @Transactional
    public String activateUser(String userId) {
        UUID userUUID = UUID.fromString(userId);
        boolean status = userQueryPort.getUserStatusById(userUUID).orElseThrow(() -> new ResourceNotFoundException("User with ID " + userId + " not found"));

        if (status) {
            throw new ResourceConflictException("User is already active");
        }

        userCommandPort.setIsActive(userUUID, true);

        return "User activated";
    }

    //TODO: test that
    @Transactional
    public UserResponse editUserAccount(UUID userId, EditUserAccountAdminRequest editRequest) {

        User user = userQueryPort.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        userQueryPort.findByEmail(editRequest.email())
                .filter(u -> !u.getId().equals(userId))
                .ifPresent(u -> {
                    throw new ResourceConflictException("User with that email is already registered");
                });


        Set<String> roles = roleQueryPort.findAllByRoleIn(editRequest.roles());

        if (editRequest.roles().size() != roles.size()) {
            throw new ResourceNotFoundException("One or more roles not found");
        }

        user.setEmail(editRequest.email());
        user.setRoles(roles);
        user.setActive(editRequest.active());

        User saved = userCommandPort.save(user);

        return mapper.toUserResponse(saved);
    }

    @Transactional
    public UserResponse editSelfAccount(EditUserAccountRequest editRequest, UUID userId) {
        User user = userQueryPort.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (editRequest.email() != null) {
            userQueryPort.findByEmail(editRequest.email())
                    .filter(u -> !u.getId().equals(userId))
                    .ifPresent(u -> {
                        throw new ResourceConflictException("User with that email is already registered");
                    });

            user.setEmail(editRequest.email());
        }

        user = userCommandPort.save(user);

        return mapper.toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public UserStatsResponse getUserStats() {
        List<UserRoleCount> roleCounts = userQueryPort.countUsersPerRole();
        List<UserStatusCount> statusCounts = userQueryPort.countUsersPerStatus();

        Map<String, Long> roleStats = roleCounts.stream()
                .collect(Collectors.toMap(UserRoleCount::getRole, UserRoleCount::getCount));

        Map<Boolean, Long> statusStats = statusCounts.stream()
                .collect(Collectors.toMap(UserStatusCount::getActive, UserStatusCount::getCount));

        Long total = 0L;

        for (var entry : statusStats.entrySet()) {
            total += entry.getValue();
        }


        return new UserStatsResponse(
                Optional.of(total).orElse(0L),
                Optional.ofNullable(statusStats.get(true)).orElse(0L),
                Optional.of(roleStats.get("ROLE_ADMIN") + roleStats.get("ROLE_REVIEW")).orElse(0L)

        );
    }
}
