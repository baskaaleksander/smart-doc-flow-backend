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

@RestController
@RequestMapping("/notification")
public class NotificationController {

    private final NotificationService notificationService;
    private final SimpUserRegistry registry;

    public NotificationController(NotificationService notificationService, SimpUserRegistry registry) {
        this.notificationService = notificationService;
        this.registry = registry;
    }

    @PostMapping("/{username}")
    public String notifyUser(@PathVariable("username") String username) {
        notificationService.sendNotification(username, "document_in_review", "lalala");

        return "done";
    }

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getAllUnreadNotifications(
            @AuthenticationPrincipal CustomUserDetails user
            ) {

        return new ResponseEntity<>(notificationService.getAllUnreadNotifications(user.getUsername()), HttpStatus.OK);
    }

}
