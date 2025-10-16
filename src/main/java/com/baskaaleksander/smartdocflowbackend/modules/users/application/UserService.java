package com.baskaaleksander.smartdocflowbackend.modules.users.application;

import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.auth.api.dto.UserResponse;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceConflictException;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.modules.users.api.dto.EditUserAccountAdminRequest;
import com.baskaaleksander.smartdocflowbackend.modules.users.api.dto.EditUserAccountRequest;
import com.baskaaleksander.smartdocflowbackend.modules.users.api.dto.UserStatsResponse;
import com.baskaaleksander.smartdocflowbackend.modules.users.domain.UserRoleCount;
import com.baskaaleksander.smartdocflowbackend.modules.users.domain.UserStatusCount;
import com.baskaaleksander.smartdocflowbackend.modules.users.mapping.UserMapper;
import com.baskaaleksander.smartdocflowbackend.modules.auth.persistence.Role;
import com.baskaaleksander.smartdocflowbackend.modules.users.persistence.User;
import com.baskaaleksander.smartdocflowbackend.modules.auth.persistence.RoleRepository;
import com.baskaaleksander.smartdocflowbackend.modules.users.persistence.UserRepository;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RoleRepository roleRepository;

    @Autowired
    public UserService(UserRepository userRepository,
                       UserMapper userMapper,
                       RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.roleRepository = roleRepository;
    }

    public PagingResult<UserResponse> getAllUsers(PaginationRequest request) {

        Pageable pageable = PaginationUtil.getPageable(request);
        Page<User> users = userRepository.findAll(pageable);

        List<UserResponse> usersList = users
                .stream()
                .map(userMapper::toUserResponse)
                .toList();

        Integer currentPage = request.getPage();
        int totalPages = users.getTotalPages();

        return new PagingResult<>(
                usersList,
                totalPages,
                users.getTotalElements(),
                users.getSize(),
                users.getNumber(),
                currentPage + 1 == totalPages,
                currentPage + 1 < totalPages
        );
    }

    public UserResponse getUserById(String userId) {
        return userMapper.toUserResponse(
                userRepository.findUserByIdWithRoles(UUID.fromString(userId)).orElseThrow(() -> new ResourceNotFoundException("User with ID " + userId + " not found"))
        );
    }

    public String inactivateUser(String userId) {

        UUID userUUID = UUID.fromString(userId);
        boolean status = userRepository.getUserStatusById(userUUID).orElseThrow(() -> new ResourceNotFoundException("User with ID " + userId + " not found"));

        if(!status) {
            throw new ResourceConflictException("User is already inactive");
        }

        userRepository.setIsActive(userUUID, false);

        return "User inactivated";
    }

    public String activateUser(String userId) {
        UUID userUUID = UUID.fromString(userId);
        boolean status = userRepository.getUserStatusById(userUUID).orElseThrow(() -> new ResourceNotFoundException("User with ID " + userId + " not found"));

        if(status) {
            throw new ResourceConflictException("User is already active");
        }

        userRepository.setIsActive(userUUID, true);

        return "User activated";
    }

    //TODO: test that
    public UserResponse editUserAccount(UUID userId, EditUserAccountAdminRequest editRequest) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Optional<User> userEmail = userRepository.findByEmail(editRequest.getEmail());

        if (userEmail.isPresent()) {
            throw new ResourceConflictException("User with that email is already registered");
        }

        Set<Role> roles = roleRepository.findAllByRoleIn(editRequest.getRoles());

        if (editRequest.getRoles().size() != roles.size()) {
            throw new ResourceNotFoundException("One or more roles not found");
        }

        user.setEmail(editRequest.getEmail());
        user.setRoles(roles);
        user.setActive(editRequest.getActive());

        userRepository.save(user);

        return userMapper.toUserResponse(user);
    }

    public UserResponse editSelfAccount(EditUserAccountRequest editRequest, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (editRequest.email() != null) {
            Optional<User> userEmail = userRepository.findByEmail(editRequest.email());

            if (userEmail.isPresent()) {
                throw new ResourceConflictException("User with that email is already registered");
            }

            user.setEmail(editRequest.email());
        }

        user = userRepository.save(user);

        return userMapper.toUserResponse(user);
    }

    public UserStatsResponse getUserStats() {
        List<UserRoleCount> roleCounts = userRepository.countUsersPerRole();
        List<UserStatusCount> statusCounts = userRepository.countUsersPerStatus();

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
