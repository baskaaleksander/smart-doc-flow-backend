package com.baskaaleksander.smartdocflowbackend.modules.notifications.application;

import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.modules.contracts.NotificationEvent;
import com.baskaaleksander.smartdocflowbackend.modules.contracts.PasswordResetEvent;
import com.baskaaleksander.smartdocflowbackend.modules.contracts.UserRegisteredEvent;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.api.NotificationApiMapper;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.api.dto.ReadNotificationRequest;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.api.dto.NotificationResponse;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PageMetadata;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagedResponse;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.model.Notification;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.model.NotificationType;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.port.*;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.mapping.NotificationMapper;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.persistence.entity.NotificationEntity;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.persistence.spring.SpringDataNotificationRepository;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationApplicationService implements NotificationEventIngressPort, AuthEventIngressPort {

    private final RealtimePushPort push;
    private final EmailSenderPort emailPort;
    private final NotificationCommandPort notificationCommandPort;
    private final NotificationQueryPort notificationQueryPort;
    private final NotificationApiMapper mapper;


    @Value(value = "${app.frontend-url}")
    private String frontendUrl;

    public NotificationApplicationService(
            RealtimePushPort push,
            EmailSenderPort emailPort,
            NotificationCommandPort notificationCommandPort,
            NotificationQueryPort notificationQueryPort,
            NotificationApiMapper mapper
            ) {
        this.push = push;
        this.emailPort = emailPort;
        this.notificationCommandPort = notificationCommandPort;
        this.notificationQueryPort = notificationQueryPort;
        this.mapper = mapper;
    }

    public PagingResult<NotificationResponse> getNotifications(String username, PaginationRequest request, Boolean read) {
        PagingResult<Notification> pagingResult;

        if (read == null) {
            pagingResult = notificationQueryPort.findAllByUsername(request, username);
        } else {
            pagingResult = notificationQueryPort.findAllByUsernameAndRead(request, username, read);
        }


        List<NotificationResponse> content = pagingResult.content().stream().map(mapper::toResponse).toList();

        return new PagingResult<>(
                content,
                pagingResult.totalPages(),
                pagingResult.totalElements(),
                pagingResult.size(),
                pagingResult.page(),
                pagingResult.last(),
                pagingResult.next()
        );
    }

    public Integer getUnreadNotificationsCount(String username) {
        return notificationQueryPort.getNotificationsCountByUsernameAndRead(username, false);
    }

    @Transactional
    public Integer markAllAsRead(String username, ReadNotificationRequest body) {

        if (body.read()) {
            return notificationCommandPort.markAllRead(username);
        }
        return 0;
    }

    @Transactional
    public Integer markOneAsRead(String username, UUID id, ReadNotificationRequest body) {
        if (body.read()) {
            return notificationCommandPort.markOneRead(username, id);
        }
        return 0;
    }


    @Override
    @Transactional
    public void onNotificationSent(NotificationEvent event) {

        Notification notification = new Notification();

        notification.setUsername(event.username());
        notification.setType(NotificationType.fromString(event.type()));
        notification.setMessage(event.message());

        notificationCommandPort.save(notification);

        push.sendToUser(event.username(), "/queue/notifications", event);
    }

    @Override
    public void onPasswordReset(PasswordResetEvent event) {
        try {
            emailPort.sendEmail(event.email(), "Password reset link", "Your password reset link is: " + frontendUrl + "/reset-password?token=" + event.token());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUserRegistered(UserRegisteredEvent event) {
        try {
            emailPort.sendEmail(event.email(), "Your password", "Your username: " + event.username() + " your password: " + event.password() + " service url: " + frontendUrl);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
