package com.baskaaleksander.smartdocflowbackend.modules.users.api;

import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.modules.auth.api.dto.UserRolesRequest;
import com.baskaaleksander.smartdocflowbackend.modules.documents.api.dto.DocumentResponse;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.auth.api.dto.UserResponse;
import com.baskaaleksander.smartdocflowbackend.common.security.CustomUserDetails;
import com.baskaaleksander.smartdocflowbackend.modules.documents.application.DocumentService;
import com.baskaaleksander.smartdocflowbackend.modules.users.application.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final DocumentService documentService;

    @Autowired
    public UserController(UserService userService, DocumentService documentService) {
        this.userService = userService;
        this.documentService = documentService;
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

    @GetMapping("/me/documents")
    public ResponseEntity<PagingResult<DocumentResponse>> getPersonalDocuments(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "id") String sortField,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @AuthenticationPrincipal CustomUserDetails user
            ) {

        PaginationRequest request = new PaginationRequest(page, size, sortField, direction);
        return new ResponseEntity<>(documentService.getUserDocuments(request, user.getId()), HttpStatus.OK);
    }

    @GetMapping("/{userId}/documents")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEW')")
    public ResponseEntity<PagingResult<DocumentResponse>> getUserDocuments(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "id") String sortField,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @PathVariable("userId") UUID userId
    ) {
        PaginationRequest request = new PaginationRequest(page, size, sortField, direction);

        return new ResponseEntity<>(documentService.getUserDocuments(request, userId), HttpStatus.OK);
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

}
