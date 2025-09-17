package com.baskaaleksander.smartdocflowbackend.controller;

import com.baskaaleksander.smartdocflowbackend.dto.response.NotificationResponse;
import com.baskaaleksander.smartdocflowbackend.security.CustomUserDetails;
import com.baskaaleksander.smartdocflowbackend.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notification")
public class NotificationController {

    private final NotificationService notificationService;
    private final SimpUserRegistry registry;

    public NotificationController(NotificationService notificationService, SimpUserRegistry registry) {
        this.notificationService = notificationService;
        this.registry = registry;
    }

    @GetMapping("/")
    public ResponseEntity<List<NotificationResponse>> getAllNotifications() {
        return null;
    }

    @PostMapping("/{id}")
    public ResponseEntity<String> markAsRead(@PathVariable("id") UUID id) {
        return null;
    }

    @PostMapping("/")
    public ResponseEntity<String> markAllAsRead() {
        return null;
    }


    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getAllUnreadNotifications(
            @AuthenticationPrincipal CustomUserDetails user
            ) {

        return new ResponseEntity<>(notificationService.getAllUnreadNotifications(user.getUsername()), HttpStatus.OK);
    }


}
