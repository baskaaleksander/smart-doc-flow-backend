package com.baskaaleksander.smartdocflowbackend.service;

import com.baskaaleksander.smartdocflowbackend.dto.response.UserResponse;
import com.baskaaleksander.smartdocflowbackend.exceptions.ResourceConflictException;
import com.baskaaleksander.smartdocflowbackend.exceptions.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.mapper.UserMapper;
import com.baskaaleksander.smartdocflowbackend.model.Role;
import com.baskaaleksander.smartdocflowbackend.model.User;
import com.baskaaleksander.smartdocflowbackend.repository.RoleRepository;
import com.baskaaleksander.smartdocflowbackend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final OpenAiChatModel chatModel;
    private final RoleRepository roleRepository;

    @Autowired
    public UserService(UserRepository userRepository,
                       UserMapper userMapper,
                       OpenAiChatModel chatModel, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.chatModel = chatModel;
        this.roleRepository = roleRepository;
    }

    public List<UserResponse> getAllUsers() {
        List<User> users = userRepository.findAll();

        return users
                .stream()
                .map(userMapper::toUserResponse)
                .toList();
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

        user.getRoles().clear();
        user.getRoles().addAll(rolesSet);

        return userMapper.toUserResponse(user);
    }

    public Map<String,String> generate(String message) {
        return Map.of("generation", this.chatModel.call(message));
    }
}
