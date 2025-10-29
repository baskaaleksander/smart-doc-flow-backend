package com.baskaaleksander.smartdocflowbackend.modules.users.application;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceConflictException;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.logging.LoggingPort;
import com.baskaaleksander.smartdocflowbackend.common.logging.Slf4jLoggingAdapter;
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
    private final LoggingPort logger;

    @Transactional(readOnly = true)
    public PagingResult<UserResponse> getAllUsers(PaginationRequest request) {
        logger.info("USR_LIST START page=" + request.getPage() + " size=" + request.getSize());
        PagingResult<User> userPagingResult = userQueryPort.findAll(request);
        List<UserResponse> content = userPagingResult.content().stream().map(mapper::toUserResponse).toList();
        logger.info("USR_LIST SUCCESS page=" + userPagingResult.page()
                + " size=" + userPagingResult.size()
                + " totalElements=" + userPagingResult.totalElements()
                + " totalPages=" + userPagingResult.totalPages());
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
        UUID userUUID = UUID.fromString(userId);

        logger.info("USR_GET START userId=" + Slf4jLoggingAdapter.shortId(userUUID));
        User user = userQueryPort.findUserByIdWithRoles(UUID.fromString(userId))
                .orElseThrow(() -> {
                    logger.warn("USR_GET FAILED reason=not_found userId=" + Slf4jLoggingAdapter.shortId(userUUID));
                    return new ResourceNotFoundException("User not found");
                });
        logger.info("USR_GET SUCCESS userId=" + Slf4jLoggingAdapter.shortId(userUUID)
                + " rolesCount=" + (user.getRoles() == null ? 0 : user.getRoles().size())
                + " active=" + user.isActive());
        return mapper.toUserResponse(user);
    }

    @Transactional
    public String inactivateUser(String userId) {
        UUID userUUID = UUID.fromString(userId);

        logger.info("USR_INACTIVATE START userId=" + Slf4jLoggingAdapter.shortId(userUUID));
        boolean status = userQueryPort.getUserStatusById(userUUID)
                .orElseThrow(() -> {
                    logger.warn("USR_INACTIVATE FAILED reason=not_found userId=" + Slf4jLoggingAdapter.shortId(userUUID));
                    return new ResourceNotFoundException("User with ID " + userId + " not found");
                });

        if (!status) {
            logger.warn("USR_INACTIVATE FAILED reason=already_inactive userId=" + Slf4jLoggingAdapter.shortId(userUUID));
            throw new ResourceConflictException("User is already inactive");
        }

        userCommandPort.setIsActive(userUUID, false);
        logger.info("USR_INACTIVATE SUCCESS userId=" + Slf4jLoggingAdapter.shortId(userUUID));
        return "User inactivated";
    }

    @Transactional
    public String activateUser(String userId) {
        UUID userUUID = UUID.fromString(userId);

        logger.info("USR_ACTIVATE START userId=" + Slf4jLoggingAdapter.shortId(userUUID));
        boolean status = userQueryPort.getUserStatusById(userUUID)
                .orElseThrow(() -> {
                    logger.warn("USR_ACTIVATE FAILED reason=not_found userId=" + Slf4jLoggingAdapter.shortId(userUUID));
                    return new ResourceNotFoundException("User with ID " + userId + " not found");
                });

        if (status) {
            logger.warn("USR_ACTIVATE FAILED reason=already_active userId=" + Slf4jLoggingAdapter.shortId(userUUID));
            throw new ResourceConflictException("User is already active");
        }

        userCommandPort.setIsActive(userUUID, true);
        logger.info("USR_ACTIVATE SUCCESS userId=" + Slf4jLoggingAdapter.shortId(userUUID));
        return "User activated";
    }

    @Transactional
    public UserResponse editUserAccount(UUID userId, EditUserAccountAdminRequest editRequest) {
        logger.info("USR_EDIT_ADMIN START userId=" + Slf4jLoggingAdapter.shortId(userId)
                + " emailHash=" + Slf4jLoggingAdapter.hashEmail(editRequest.email())
                + " rolesRequested=" + (editRequest.roles() == null ? 0 : editRequest.roles().size()));

        User user = userQueryPort.findById(userId)
                .orElseThrow(() -> {
                    logger.warn("USR_EDIT_ADMIN FAILED reason=user_not_found userId=" + Slf4jLoggingAdapter.shortId(userId));
                    return new ResourceNotFoundException("User not found");
                });

        userQueryPort.findByEmail(editRequest.email())
                .filter(u -> !u.getId().equals(userId))
                .ifPresent(u -> {
                    logger.warn("USR_EDIT_ADMIN FAILED reason=email_conflict userId=" + Slf4jLoggingAdapter.shortId(userId)
                            + " emailHash=" + Slf4jLoggingAdapter.hashEmail(editRequest.email()));
                    throw new ResourceConflictException("User with that email is already registered");
                });

        Set<String> roles = roleQueryPort.findAllByRoleIn(editRequest.roles());
        if (editRequest.roles().size() != roles.size()) {
            logger.warn("USR_EDIT_ADMIN FAILED reason=role_not_found userId=" + Slf4jLoggingAdapter.shortId(userId)
                    + " requested=" + editRequest.roles().size() + " found=" + roles.size());
            throw new ResourceNotFoundException("One or more roles not found");
        }

        user.setEmail(editRequest.email());
        user.setRoles(roles);
        user.setActive(editRequest.active());

        User saved = userCommandPort.save(user);
        logger.info("USR_EDIT_ADMIN SUCCESS userId=" + Slf4jLoggingAdapter.shortId(userId)
                + " roles=" + roles.size()
                + " active=" + saved.isActive());
        return mapper.toUserResponse(saved);
    }

    @Transactional
    public UserResponse editSelfAccount(EditUserAccountRequest editRequest, UUID userId) {
        logger.info("USR_EDIT_SELF START userId=" + Slf4jLoggingAdapter.shortId(userId)
                + " emailProvided=" + (editRequest.email() != null));

        User user = userQueryPort.findById(userId)
                .orElseThrow(() -> {
                    logger.warn("USR_EDIT_SELF FAILED reason=user_not_found userId=" + Slf4jLoggingAdapter.shortId(userId));
                    return new ResourceNotFoundException("User not found");
                });

        if (editRequest.email() != null) {
            userQueryPort.findByEmail(editRequest.email())
                    .filter(u -> !u.getId().equals(userId))
                    .ifPresent(u -> {
                        logger.warn("USR_EDIT_SELF FAILED reason=email_conflict userId=" + Slf4jLoggingAdapter.shortId(userId)
                                + " emailHash=" + Slf4jLoggingAdapter.hashEmail(editRequest.email()));
                        throw new ResourceConflictException("User with that email is already registered");
                    });

            user.setEmail(editRequest.email());
        }

        user = userCommandPort.save(user);
        logger.info("USR_EDIT_SELF SUCCESS userId=" + Slf4jLoggingAdapter.shortId(userId)
                + " emailChanged=" + (editRequest.email() != null));
        return mapper.toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public UserStatsResponse getUserStats() {
        logger.info("USR_STATS START");
        List<UserRoleCount> roleCounts = userQueryPort.countUsersPerRole();
        List<UserStatusCount> statusCounts = userQueryPort.countUsersPerStatus();

        Map<String, Long> roleStats = roleCounts.stream()
                .collect(Collectors.toMap(UserRoleCount::getRole, UserRoleCount::getCount));
        Map<Boolean, Long> statusStats = statusCounts.stream()
                .collect(Collectors.toMap(UserStatusCount::getActive, UserStatusCount::getCount));

        long total = statusStats.values().stream().mapToLong(Long::longValue).sum();
        long active = Optional.ofNullable(statusStats.get(true)).orElse(0L);
        long reviewers = Optional.ofNullable(roleStats.get("ROLE_ADMIN")).orElse(0L)
                + Optional.ofNullable(roleStats.get("ROLE_REVIEW")).orElse(0L);

        logger.info("USR_STATS SUCCESS total=" + total + " active=" + active + " reviewers=" + reviewers);

        return new UserStatsResponse(
                total,
                active,
                reviewers
        );
    }
}