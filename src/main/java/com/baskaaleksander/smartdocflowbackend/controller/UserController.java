package com.baskaaleksander.smartdocflowbackend.controller;

import com.baskaaleksander.smartdocflowbackend.dto.request.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.dto.request.UserRolesRequest;
import com.baskaaleksander.smartdocflowbackend.dto.response.PagingResult;
import com.baskaaleksander.smartdocflowbackend.dto.response.UserResponse;
import com.baskaaleksander.smartdocflowbackend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public PagingResult<UserResponse> getAllUsers(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "id") String sortField,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ){
        PaginationRequest request = new PaginationRequest(page, size, sortField, direction);

        return userService.getAllUsers(request);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("@userAccess.canManage(#userId, authentication)")
    public ResponseEntity<UserResponse> getUserById(@PathVariable("userId") String userId) {
        return new ResponseEntity<>(userService.getUserById(userId), HttpStatus.OK);
    }

    @PatchMapping("/{userId}/roles")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<UserResponse> assignRoles(@PathVariable("userId") UUID userId, @RequestBody @Valid UserRolesRequest userRolesRequest) {
        return new ResponseEntity<>(userService.updateUserRoles(userId, userRolesRequest.getRoles()), HttpStatus.OK);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("@userAccess.canManage(#userId, authentication)")
    public ResponseEntity<String> inactivateUserAccount(@PathVariable("userId") String userId) {
        return new ResponseEntity<>(userService.inactivateUser(userId), HttpStatus.OK);
    }

    @PostMapping("/{userId}/restore")
    @PreAuthorize("@userAccess.canManage(#userId, authentication)")
    public ResponseEntity<String> restoreUserAccount(@PathVariable("userId") String userId) {
        return new ResponseEntity<>(userService.activateUser(userId), HttpStatus.OK);
    }

    @GetMapping("/ai")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Map<String,String> generate(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        return userService.generate(message);
    }
}
