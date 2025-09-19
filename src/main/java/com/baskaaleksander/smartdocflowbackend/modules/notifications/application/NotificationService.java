package com.baskaaleksander.smartdocflowbackend.modules.notifications.application;

import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.api.dto.ReadNotificationRequest;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.api.dto.NotificationResponse;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PageMetadata;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagedResponse;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.NotificationType;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.mapping.NotificationMapper;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.persistence.Notification;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.persistence.NotificationRepository;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationUtil;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final NotificationMapper notificationMapper;

    public NotificationService(
            NotificationRepository notificationRepository,
            SimpMessagingTemplate simpMessagingTemplate,
            NotificationMapper notificationMapper) {
        this.notificationRepository = notificationRepository;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.notificationMapper = notificationMapper;
    }

    @Transactional
    public void sendNotification(String username, String type, String message) {
        Notification notification = new Notification();

        notification.setUsername(username);
        notification.setType(NotificationType.fromString(type));
        notification.setMessage(message);

        notificationRepository.save(notification);

        simpMessagingTemplate.convertAndSendToUser(
                username,
        "/queue/notifications",
                notification
                );

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
        Page<Notification> notifications = notificationRepository.findAllByUsernameAndRead(pageable, username, read);

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
        Page<Notification> notifications = notificationRepository.findAllByUsername(pageable, username);

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


}
