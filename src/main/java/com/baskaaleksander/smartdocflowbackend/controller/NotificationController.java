package com.baskaaleksander.smartdocflowbackend.controller;

import com.baskaaleksander.smartdocflowbackend.dto.request.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.dto.request.ReadNotificationRequest;
import com.baskaaleksander.smartdocflowbackend.dto.response.NotificationResponse;
import com.baskaaleksander.smartdocflowbackend.dto.response.PagingResult;
import com.baskaaleksander.smartdocflowbackend.security.CustomUserDetails;
import com.baskaaleksander.smartdocflowbackend.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/notification")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
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
    public ResponseEntity<Integer> getUnreadNotificationsCount() {
        return null;
    }

    @PatchMapping("/{id}")
    public ResponseEntity<String> markAsRead(@PathVariable("id") UUID id, @RequestBody @Valid ReadNotificationRequest body) {
        return null;
    }

    @PatchMapping("")
    public ResponseEntity<String> markAllAsRead(@RequestBody @Valid ReadNotificationRequest body) {
        return null;
    }


}
