package com.baskaaleksander.smartdocflowbackend.service;

import com.baskaaleksander.smartdocflowbackend.dto.response.UserResponse;
import com.baskaaleksander.smartdocflowbackend.exceptions.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.mapper.UserMapper;
import com.baskaaleksander.smartdocflowbackend.model.User;
import com.baskaaleksander.smartdocflowbackend.repository.UserRepository;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;


@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final OpenAiChatModel chatModel;

    @Autowired
    public UserService(UserRepository userRepository,
                       UserMapper userMapper,
                       OpenAiChatModel chatModel) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.chatModel = chatModel;
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

    public Map<String,String> generate(String message) {
        return Map.of("generation", this.chatModel.call(message));
    }
}
