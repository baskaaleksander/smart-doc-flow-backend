package com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.api;

import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.api.dto.ReadNotificationRequest;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.api.dto.NotificationResponse;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.common.security.CustomUserDetails;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.application.NotificationApplicationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


import java.util.UUID;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationApplicationService notificationService;

    public NotificationController(NotificationApplicationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("")
    public ResponseEntity<PagingResult<NotificationResponse>> getNotifications(
            @RequestParam(required = false) Boolean read,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "id") String sortField,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PaginationRequest request = new PaginationRequest(page, size, sortField, direction);
        return new ResponseEntity<>(notificationService.getNotifications(userDetails.getUsername(), request, read), HttpStatus.OK);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Integer> getUnreadNotificationsCount(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return new ResponseEntity<>(notificationService.getUnreadNotificationsCount(userDetails.getUsername()), HttpStatus.OK);
    }

    @PatchMapping("")
    public ResponseEntity<Integer> markAllAsRead(@RequestBody @Valid ReadNotificationRequest body, @AuthenticationPrincipal CustomUserDetails userDetails) {
        return new ResponseEntity<>(notificationService.markAllAsRead(userDetails.getUsername(), body), HttpStatus.OK);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Integer> markAsRead(@PathVariable("id") UUID id, @RequestBody @Valid ReadNotificationRequest body, @AuthenticationPrincipal CustomUserDetails userDetails) {
        return new ResponseEntity<>(notificationService.markOneAsRead(userDetails.getUsername(), id, body), HttpStatus.OK);
    }




}
