package com.baskaaleksander.smartdocflowbackend.modules.users.application;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceConflictException;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api.dto.UserResponse;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.entity.RoleEntity;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.spring.SpringDataRoleRepository;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.api.UserMapper;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.entity.UserEntity;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.spring.SpringDataUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import static org.assertj.core.api.Assertions.assertThat;


import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private SpringDataUserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private SpringDataRoleRepository roleRepository;

    @InjectMocks
    private UserApplicationService userService;

    private UserEntity user1;

    @BeforeEach
    void setUp() {
        RoleEntity role = new RoleEntity();
        role.setRole("ROLE_USER");
        user1 = new UserEntity();
        user1.setUsername("u1");
        user1.setEmail("u1@example.com");
        user1.setId(UUID.randomUUID());
        user1.setRoles(Set.of(role));
        user1.setActive(true);
        user1.setCreatedAt(Instant.now());
    }


    @Test
    void getAllUsers_shouldReturnPagingResult() {
        Page<UserEntity> page = new PageImpl<>(List.of(user1), PageRequest.of(0, 10), 25);
        when(userRepository.findAll(any(Pageable.class))).thenReturn(page);

        UserResponse dto1 = new UserResponse(UUID.randomUUID(), user1.getUsername(), user1.getEmail(), List.of("ROLE_USER"), true, user1.getCreatedAt());

        when(userMapper.toUserResponse(user1)).thenReturn(dto1);

        PagingResult<UserResponse> response = userService.getAllUsers(new PaginationRequest(0, 10, "id", Sort.Direction.DESC));

        assertThat(response.content()).contains(dto1);
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(25);
        assertThat(response.last()).isEqualTo(false);
        assertThat(response.next()).isEqualTo(true);
    }

    @Test
    void getUserById_shouldThrowException() {
        UUID id = UUID.randomUUID();
        when(userRepository.findUserByIdWithRoles(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(id.toString())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getUserById_shouldReturnUserResponse() {
        UserResponse mapped = new UserResponse(UUID.randomUUID(), user1.getUsername(), user1.getEmail(), List.of("ROLE_USER"), true, user1.getCreatedAt());
        when(userRepository.findUserByIdWithRoles(user1.getId())).thenReturn(Optional.of(user1));
        when(userMapper.toUserResponse(user1)).thenReturn(mapped);

        UserResponse res = userService.getUserById(user1.getId().toString());

        assertThat(res.id()).isEqualTo(mapped.id());
        assertThat(res.username()).isEqualTo(mapped.username());
        assertThat(res.roles()).isEqualTo(mapped.roles());
        assertThat(res.active()).isEqualTo(mapped.active());
    }

    @Test
    void inactivateUser_shouldThrowException_whenStatusNotFound() {
        UUID id = UUID.randomUUID();

        when(userRepository.getUserStatusById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.inactivateUser(id.toString()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void inactivateUser_shouldThrowException_whenStatusFalse() {
        UUID id = UUID.randomUUID();

        when(userRepository.getUserStatusById(id)).thenReturn(Optional.of(false));

        assertThatThrownBy(() -> userService.inactivateUser(id.toString()))
                .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void inactivateUser_shouldSuccess() {
        UUID id = UUID.randomUUID();

        when(userRepository.getUserStatusById(id)).thenReturn(Optional.of(true));

        String res = userService.inactivateUser(id.toString());

        verify(userRepository).setIsActive(id, false);
        assertThat(res).isEqualTo("User inactivated");

    }

    @Test
    void activateUser_shouldThrowException_whenStatusNotFound() {
        UUID id = UUID.randomUUID();

        when(userRepository.getUserStatusById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.activateUser(id.toString()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void activateUser_shouldThrowException_whenStatusFalse() {
        UUID id = UUID.randomUUID();

        when(userRepository.getUserStatusById(id)).thenReturn(Optional.of(true));

        assertThatThrownBy(() -> userService.activateUser(id.toString()))
                .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void activateUser_shouldSuccess() {
        UUID id = UUID.randomUUID();

        when(userRepository.getUserStatusById(id)).thenReturn(Optional.of(false));

        String res = userService.activateUser(id.toString());

        verify(userRepository).setIsActive(id, true);
        assertThat(res).isEqualTo("User activated");

    }

//    @Test
//    void updateUserRoles_shouldThrowException_whenRoleNotFound() {
//        Set<String> roles = Set.of("ROLE_USER");
//
//        when(roleRepository.findAllByRoleIn(roles)).thenReturn(Set.of());
//
//        assertThatThrownBy(() -> userService.updateUserRoles(UUID.randomUUID(), roles))
//                .isInstanceOf(ResourceNotFoundException.class);
//
//    }
//
//    @Test
//    void updateUserRoles_shouldThrowException_whenUserNotFound() {
//        Set<String> roles = Set.of("ROLE_USER");
//        UUID id = UUID.randomUUID();
//
//        when(roleRepository.findAllByRoleIn(roles)).thenReturn(Set.of(new Role()));
//        when(userRepository.findUserByIdWithRoles(id)).thenReturn(Optional.empty());
//        assertThatThrownBy(() -> userService.updateUserRoles(id, roles))
//                .isInstanceOf(ResourceNotFoundException.class);
//
//    }
//
//    @Test
//    void updateUserRoles_shouldReturnUserResponse() {
//
//        Set<String> roles = Set.of("ROLE_USER");
//        Role userRole = new Role();
//        userRole.setRole("ROLE_USER");
//
//        user1.setRoles(new HashSet<>(Set.of(userRole)));
//
//        when(roleRepository.findAllByRoleIn(roles)).thenReturn(Set.of(userRole));
//        when(userRepository.findUserByIdWithRoles(user1.getId())).thenReturn(Optional.of(user1));
//        when(userRepository.save(user1)).thenReturn(user1);
//
//        UserResponse mapped = new UserResponse(UUID.randomUUID(), user1.getUsername(), user1.getEmail(), List.of("ROLE_USER"), true, user1.getCreatedAt());
//
//        when(userMapper.toUserResponse(user1))
//                .thenReturn(mapped);
//
//        UserResponse res = userService.updateUserRoles(user1.getId(), roles);
//
//        assertThat(res.username()).isEqualTo(mapped.username());
//        assertThat(res.username()).isEqualTo(mapped.username());
//        assertThat(res.roles()).containsExactly("ROLE_USER");
//    }
}
