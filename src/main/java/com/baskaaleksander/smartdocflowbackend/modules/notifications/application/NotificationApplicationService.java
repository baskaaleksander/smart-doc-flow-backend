package com.baskaaleksander.smartdocflowbackend.modules.notifications.application;

import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.modules.contracts.NotificationEvent;
import com.baskaaleksander.smartdocflowbackend.modules.contracts.PasswordResetEvent;
import com.baskaaleksander.smartdocflowbackend.modules.contracts.UserRegisteredEvent;
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

    private final SpringDataNotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final RealtimePushPort push;
    private final EmailSenderPort emailPort;
    private final NotificationCommandPort notificationCommandPort;
    private final NotificationQueryPort notificationQueryPort;

    @Value(value = "${app.frontend-url}")
    private String frontendUrl;

    public NotificationApplicationService(
            SpringDataNotificationRepository notificationRepository,
            NotificationMapper notificationMapper,
            RealtimePushPort push,
            EmailSenderPort emailPort,
            NotificationCommandPort notificationCommandPort,
            NotificationQueryPort notificationQueryPort
            ) {
        this.notificationRepository = notificationRepository;
        this.notificationMapper = notificationMapper;
        this.push = push;
        this.emailPort = emailPort;
        this.notificationCommandPort = notificationCommandPort;
        this.notificationQueryPort = notificationQueryPort;
    }

    public PagingResult<NotificationResponse> getNotifications(String username, PaginationRequest request, Boolean read) {
        Pageable pageable = PaginationUtil.getPageable(request);
        PagedResponse result;

        if (read == null) {
            result = getAllUserNotifications(pageable, username);
        } else {
            result = getAllUserNotificationsByRead(pageable, username, read);
        }


        List<NotificationResponse> items = result.items();
        PageMetadata pageMetadata = result.pageMetadata();
        Integer currentPage = request.getPage();

        return new PagingResult<>(
                items,
                pageMetadata.totalPages(),
                pageMetadata.totalElements(),
                pageMetadata.size(),
                pageMetadata.number(),
                currentPage + 1 == pageMetadata.totalPages(),
                currentPage + 1 < pageMetadata.totalPages()

        );
    }

    private PagedResponse<NotificationResponse> getAllUserNotificationsByRead(Pageable pageable, String username, Boolean read) {
        Page<NotificationEntity> notifications = notificationRepository.findAllByUsernameAndRead(pageable, username, read);

        List<NotificationResponse> notificationsList = notifications
                .stream()
                .map(notificationMapper::toNotificationResponse)
                .toList();
        PageMetadata pageMetadata = new PageMetadata(
                notifications.getTotalPages(),
                notifications.getTotalElements(),
                notifications.getSize(),
                notifications.getNumber()
        );

        return new PagedResponse<>(notificationsList, pageMetadata);
    }

    private PagedResponse<NotificationResponse> getAllUserNotifications(Pageable pageable, String username) {
        Page<NotificationEntity> notifications = notificationRepository.findAllByUsername(pageable, username);

        List<NotificationResponse> notificationsList = notifications
                .stream()
                .map(notificationMapper::toNotificationResponse)
                .toList();

        PageMetadata pageMetadata = new PageMetadata(
                notifications.getTotalPages(),
                notifications.getTotalElements(),
                notifications.getSize(),
                notifications.getNumber()
        );

        return new PagedResponse<>(notificationsList, pageMetadata);
    }

    public Integer getUnreadNotificationsCount(String username) {
        return notificationRepository.getNotificationsCountByUsernameAndRead(username, false);
    }

    @Transactional
    public Integer markAllAsRead(String username, ReadNotificationRequest body) {

        if (body.getRead()) {
            return notificationRepository.markAllRead(username);
        }
        return 0;
    }

    @Transactional
    public Integer markOneAsRead(String username, UUID id, ReadNotificationRequest body) {
        if (body.getRead()) {
            return notificationRepository.markOneRead(username, id);
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

        //add save to db
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
