package com.baskaaleksander.smartdocflowbackend.controller;

import com.baskaaleksander.smartdocflowbackend.service.NotificationService;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.web.bind.annotation.*;

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

}
