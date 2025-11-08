package com.baskaaleksander.smartdocflowbackend.modules.users.application;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceConflictException;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.logging.LoggingPort;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api.dto.AuthActionResponse;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api.dto.UserResponse;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.api.UserApiMapper;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.api.dto.AccountActionResponse;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.api.dto.EditUserAccountAdminRequest;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.api.dto.EditUserAccountRequest;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.api.dto.UserStatsResponse;
import com.baskaaleksander.smartdocflowbackend.modules.users.domain.model.User;
import com.baskaaleksander.smartdocflowbackend.modules.users.domain.port.UserCommandPort;
import com.baskaaleksander.smartdocflowbackend.modules.users.domain.port.UserQueryPort;
import com.baskaaleksander.smartdocflowbackend.modules.users.domain.port.UserRoleQueryPort;
import com.baskaaleksander.smartdocflowbackend.modules.users.domain.view.UserRoleCount;
import com.baskaaleksander.smartdocflowbackend.modules.users.domain.view.UserStatusCount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserApplicationServiceTest {

    @Mock
    private UserQueryPort userQueryPort;
    @Mock
    private UserCommandPort userCommandPort;
    @Mock
    private UserRoleQueryPort roleQueryPort;
    @Mock
    private UserApiMapper mapper;
    @Mock
    private LoggingPort logger;

    @InjectMocks
    private UserApplicationService service;

    private UUID userId;
    private User user;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        Instant createdAt = Instant.now();
        user.setId(userId);
        user.setUsername("john");
        user.setEmail("john@doe.com");
        user.setActive(true);
        user.setRoles(new HashSet<>(Set.of("ROLE_USER")));
        user.setCreatedAt(createdAt);
        userResponse = new UserResponse(userId, "john", "john@doe.com", List.of("ROLE_USER"), true, createdAt);
    }

    @Test
    void getAllUsers_success() {
        PaginationRequest req = new PaginationRequest(0, 2, "email", Sort.Direction.ASC);
        PagingResult<User> page = new PagingResult<>(List.of(user), 1, 1L, 2, 0, true, false);
        when(userQueryPort.findAll(req)).thenReturn(page);
        when(mapper.toUserResponse(user)).thenReturn(userResponse);

        PagingResult<UserResponse> result = service.getAllUsers(req);

        assertThat(result.content()).containsExactly(userResponse);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void getUserById_success() {
        when(userQueryPort.findUserByIdWithRoles(userId)).thenReturn(Optional.of(user));
        when(mapper.toUserResponse(user)).thenReturn(userResponse);

        UserResponse result = service.getUserById(userId.toString());

        assertThat(result).isEqualTo(userResponse);
    }

    @Test
    void getUserById_notFound() {
        when(userQueryPort.findUserByIdWithRoles(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUserById(userId.toString()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void inactivateUser_success() {
        when(userQueryPort.getUserStatusById(userId)).thenReturn(Optional.of(true));

        AccountActionResponse result = service.inactivateUser(userId.toString());

        assertThat(result.completed()).isTrue();
        verify(userCommandPort).setIsActive(userId, false);
    }

    @Test
    void inactivateUser_alreadyInactive() {
        when(userQueryPort.getUserStatusById(userId)).thenReturn(Optional.of(false));

        assertThatThrownBy(() -> service.inactivateUser(userId.toString()))
                .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void inactivateUser_notFound() {
        when(userQueryPort.getUserStatusById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.inactivateUser(userId.toString()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void activateUser_success() {
        when(userQueryPort.getUserStatusById(userId)).thenReturn(Optional.of(false));

        AccountActionResponse result = service.activateUser(userId.toString());

        assertThat(result.completed()).isTrue();
        verify(userCommandPort).setIsActive(userId, true);
    }

    @Test
    void activateUser_alreadyActive() {
        when(userQueryPort.getUserStatusById(userId)).thenReturn(Optional.of(true));

        assertThatThrownBy(() -> service.activateUser(userId.toString()))
                .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void activateUser_notFound() {
        when(userQueryPort.getUserStatusById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activateUser(userId.toString()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void editUserAccount_success() {
        EditUserAccountAdminRequest req = new EditUserAccountAdminRequest("new@mail.com", Set.of("ROLE_USER", "ROLE_REVIEW"), false);
        when(userQueryPort.findById(userId)).thenReturn(Optional.of(user));
        when(userQueryPort.findByEmail("new@mail.com")).thenReturn(Optional.empty());
        when(roleQueryPort.findAllByRoleIn(req.roles())).thenReturn(new HashSet<>(req.roles()));
        when(userCommandPort.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toUserResponse(any(User.class))).thenReturn(userResponse);

        UserResponse result = service.editUserAccount(userId, req);

        assertThat(result).isEqualTo(userResponse);
        verify(userCommandPort).save(any(User.class));
    }

    @Test
    void editUserAccount_userNotFound() {
        EditUserAccountAdminRequest req = new EditUserAccountAdminRequest("a@a.com", Set.of("ROLE_USER"), true);
        when(userQueryPort.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.editUserAccount(userId, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void editUserAccount_emailTaken() {
        EditUserAccountAdminRequest req = new EditUserAccountAdminRequest("dup@mail.com", Set.of("ROLE_USER"), true);
        when(userQueryPort.findById(userId)).thenReturn(Optional.of(user));
        User other = new User();
        other.setId(UUID.randomUUID());
        other.setEmail("dup@mail.com");
        when(userQueryPort.findByEmail("dup@mail.com")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.editUserAccount(userId, req))
                .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void editUserAccount_roleMissing() {
        EditUserAccountAdminRequest req = new EditUserAccountAdminRequest("a@a.com", Set.of("ROLE_USER", "ROLE_X"), true);
        when(userQueryPort.findById(userId)).thenReturn(Optional.of(user));
        when(userQueryPort.findByEmail("a@a.com")).thenReturn(Optional.empty());
        when(roleQueryPort.findAllByRoleIn(req.roles())).thenReturn(new HashSet<>(Set.of("ROLE_USER")));

        assertThatThrownBy(() -> service.editUserAccount(userId, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void editSelfAccount_success() {
        EditUserAccountRequest req = new EditUserAccountRequest("self@mail.com");
        when(userQueryPort.findById(userId)).thenReturn(Optional.of(user));
        when(userQueryPort.findByEmail("self@mail.com")).thenReturn(Optional.empty());
        when(userCommandPort.save(any(User.class))).thenReturn(user);
        when(mapper.toUserResponse(user)).thenReturn(userResponse);

        UserResponse result = service.editSelfAccount(req, userId);

        assertThat(result).isEqualTo(userResponse);
    }

    @Test
    void editSelfAccount_userNotFound() {
        when(userQueryPort.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.editSelfAccount(new EditUserAccountRequest("x@x.com"), userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void editSelfAccount_emailTaken() {
        EditUserAccountRequest req = new EditUserAccountRequest("dup@mail.com");
        when(userQueryPort.findById(userId)).thenReturn(Optional.of(user));
        User other = new User();
        other.setId(UUID.randomUUID());
        other.setEmail("dup@mail.com");
        when(userQueryPort.findByEmail("dup@mail.com")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.editSelfAccount(req, userId))
                .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void editSelfAccount_emailNull() {
        EditUserAccountRequest req = new EditUserAccountRequest(null);
        when(userQueryPort.findById(userId)).thenReturn(Optional.of(user));
        when(userCommandPort.save(user)).thenReturn(user);
        when(mapper.toUserResponse(user)).thenReturn(userResponse);

        UserResponse result = service.editSelfAccount(req, userId);

        assertThat(result).isEqualTo(userResponse);
        verify(userQueryPort, never()).findByEmail(anyString());
    }

    @Test
    void getUserStats_success() {
        UserRoleCount rcAdmin = mock(UserRoleCount.class);
        when(rcAdmin.getRole()).thenReturn("ROLE_ADMIN");
        when(rcAdmin.getCount()).thenReturn(2L);
        UserRoleCount rcReview = mock(UserRoleCount.class);
        when(rcReview.getRole()).thenReturn("ROLE_REVIEW");
        when(rcReview.getCount()).thenReturn(3L);
        UserStatusCount scActive = mock(UserStatusCount.class);
        when(scActive.getActive()).thenReturn(true);
        when(scActive.getCount()).thenReturn(4L);
        UserStatusCount scInactive = mock(UserStatusCount.class);
        when(scInactive.getActive()).thenReturn(false);
        when(scInactive.getCount()).thenReturn(1L);
        when(userQueryPort.countUsersPerRole()).thenReturn(List.of(rcAdmin, rcReview));
        when(userQueryPort.countUsersPerStatus()).thenReturn(List.of(scActive, scInactive));

        UserStatsResponse stats = service.getUserStats();

        assertThat(stats.total()).isEqualTo(5L);
        assertThat(stats.active()).isEqualTo(4L);
        assertThat(stats.adminsReviewers()).isEqualTo(5L);
    }

    @Test
    void getUserStats_missingActiveKey() {
        UserRoleCount rcAdmin = mock(UserRoleCount.class);
        when(rcAdmin.getRole()).thenReturn("ROLE_ADMIN");
        when(rcAdmin.getCount()).thenReturn(1L);
        UserRoleCount rcReview = mock(UserRoleCount.class);
        when(rcReview.getRole()).thenReturn("ROLE_REVIEW");
        when(rcReview.getCount()).thenReturn(0L);
        UserStatusCount scInactive = mock(UserStatusCount.class);
        when(scInactive.getActive()).thenReturn(false);
        when(scInactive.getCount()).thenReturn(7L);
        when(userQueryPort.countUsersPerRole()).thenReturn(List.of(rcAdmin, rcReview));
        when(userQueryPort.countUsersPerStatus()).thenReturn(List.of(scInactive));

        UserStatsResponse stats = service.getUserStats();

        assertThat(stats.total()).isEqualTo(7L);
        assertThat(stats.active()).isEqualTo(0L);
        assertThat(stats.adminsReviewers()).isEqualTo(1L);
    }
}