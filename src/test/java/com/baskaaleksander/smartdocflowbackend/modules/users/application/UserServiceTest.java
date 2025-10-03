package com.baskaaleksander.smartdocflowbackend.modules.users.application;

import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.auth.api.dto.UserResponse;
import com.baskaaleksander.smartdocflowbackend.modules.auth.persistence.RoleRepository;
import com.baskaaleksander.smartdocflowbackend.modules.users.mapping.UserMapper;
import com.baskaaleksander.smartdocflowbackend.modules.users.persistence.User;
import com.baskaaleksander.smartdocflowbackend.modules.users.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import static org.assertj.core.api.Assertions.assertThat;



import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserService userService;


    @Test
    void getAllUsers_shouldReturnPagingResult() {
        User user1 = new User();
        user1.setUsername("u1");
        Page<User> page = new PageImpl<>(List.of(user1), PageRequest.of(0, 10), 25);
        when(userRepository.findAll(any(Pageable.class))).thenReturn(page);

        UserResponse dto1 = new UserResponse(UUID.randomUUID(), user1.getUsername(), List.of("ROLE_USER"), true);

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

}
