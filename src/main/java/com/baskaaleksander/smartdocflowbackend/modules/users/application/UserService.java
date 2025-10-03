package com.baskaaleksander.smartdocflowbackend.modules.users.application;

import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.auth.api.dto.UserResponse;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceConflictException;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.modules.users.mapping.UserMapper;
import com.baskaaleksander.smartdocflowbackend.modules.auth.persistence.Role;
import com.baskaaleksander.smartdocflowbackend.modules.users.persistence.User;
import com.baskaaleksander.smartdocflowbackend.modules.auth.persistence.RoleRepository;
import com.baskaaleksander.smartdocflowbackend.modules.users.persistence.UserRepository;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationUtil;
import jakarta.transaction.Transactional;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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

    @Transactional
    public UserResponse updateUserRoles(UUID userId, Set<String> roleNames) {

        Set<Role> rolesSet = roleNames.stream()
                .map(name -> roleRepository.findRoleByRole(name)
                        .orElseThrow(() -> new ResourceNotFoundException("Role " + name + " not found")))
                .collect(Collectors.toSet());

        User user = userRepository.findUserByIdWithRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + userId + " not found"));

        user.setRoles(rolesSet);

        user = userRepository.save(user);

        return userMapper.toUserResponse(user);
    }
}
