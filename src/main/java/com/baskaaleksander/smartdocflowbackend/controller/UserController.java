package com.baskaaleksander.smartdocflowbackend.controller;

import com.baskaaleksander.smartdocflowbackend.dto.response.UserResponse;
import com.baskaaleksander.smartdocflowbackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
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
    public List<UserResponse> getAllUsers(){
        return userService.getAllUsers();
    }

    @GetMapping("/{userId}")
    @PreAuthorize("@userAccess.canManage(#userId, authentication)")
    public ResponseEntity<UserResponse> getUserById(@PathVariable("userId") String userId) {
        return new ResponseEntity<>(userService.getUserById(userId), HttpStatus.OK);
    }

    @PatchMapping("/{userId}/roles")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<UserResponse> assignRoles(@PathVariable("userId") UUID userId) {
        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("@userAccess.canManage(#userId, authentication)")
    public ResponseEntity<String> inactivateUserAccount(@PathVariable("userId") String userId) {
        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    @PostMapping("/{userId}/restore")
    @PreAuthorize("@userAccess.canManage(#userId, authentication)")
    public ResponseEntity<UserResponse> restoreUserAccount(@PathVariable("userId") String userId) {
        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    @GetMapping("/ai")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Map<String,String> generate(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        return userService.generate(message);
    }
}
