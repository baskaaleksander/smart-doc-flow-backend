package com.baskaaleksander.smartdocflowbackend.modules.notifications.application;

import com.baskaaleksander.smartdocflowbackend.common.logging.LoggingPort;
import com.baskaaleksander.smartdocflowbackend.common.logging.Slf4jLoggingAdapter;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.contracts.NotificationEvent;
import com.baskaaleksander.smartdocflowbackend.modules.contracts.PasswordResetEvent;
import com.baskaaleksander.smartdocflowbackend.modules.contracts.UserRegisteredEvent;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.api.NotificationApiMapper;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.api.dto.NotificationResponse;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.api.dto.ReadNotificationRequest;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.model.Notification;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.model.NotificationType;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.port.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationApplicationService implements NotificationEventIngressPort, AuthEventIngressPort {

    private final RealtimePushPort push;
    private final EmailSenderPort emailPort;
    private final NotificationCommandPort notificationCommandPort;
    private final NotificationQueryPort notificationQueryPort;
    private final NotificationApiMapper mapper;
    private final LoggingPort logger;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public PagingResult<NotificationResponse> getNotifications(String username, PaginationRequest request, Boolean read) {
        logger.info("NOTIF_LIST START username=" + username + " page=" + request.getPage() + " size=" + request.getSize() + " read=" + read);

        PagingResult<Notification> pagingResult = (read == null)
                ? notificationQueryPort.findAllByUsername(request, username)
                : notificationQueryPort.findAllByUsernameAndRead(request, username, read);

        List<NotificationResponse> content = pagingResult.content().stream().map(mapper::toResponse).toList();

        logger.info("NOTIF_LIST SUCCESS username=" + username
                + " count=" + content.size()
                + " totalElements=" + pagingResult.totalElements()
                + " totalPages=" + pagingResult.totalPages());

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
        logger.info("NOTIF_UNREAD_COUNT START username=" + username);
        Integer count = notificationQueryPort.getNotificationsCountByUsernameAndRead(username, false);
        logger.info("NOTIF_UNREAD_COUNT SUCCESS username=" + username + " count=" + count);
        return count;
    }

    @Transactional
    public Integer markAllAsRead(String username, ReadNotificationRequest body) {
        logger.info("NOTIF_MARK_ALL START username=" + username + " read=" + body.read());
        if (body.read()) {
            int updated = notificationCommandPort.markAllRead(username);
            logger.info("NOTIF_MARK_ALL SUCCESS username=" + username + " updated=" + updated);
            return updated;
        }
        logger.info("NOTIF_MARK_ALL NOOP username=" + username + " read=false");
        return 0;
    }

    @Transactional
    public Integer markOneAsRead(String username, UUID id, ReadNotificationRequest body) {
        logger.info("NOTIF_MARK_ONE START username=" + username + " id=" + Slf4jLoggingAdapter.shortId(id) + " read=" + body.read());
        if (body.read()) {
            int updated = notificationCommandPort.markOneRead(username, id);
            logger.info("NOTIF_MARK_ONE SUCCESS username=" + username + " id=" + Slf4jLoggingAdapter.shortId(id) + " updated=" + updated);
            return updated;
        }
        logger.info("NOTIF_MARK_ONE NOOP username=" + username + " id=" + Slf4jLoggingAdapter.shortId(id) + " read=false");
        return 0;
    }

    @Override
    @Transactional
    public void onNotificationSent(NotificationEvent event) {
        logger.info("NOTIF_EVENT START username=" + event.username() + " type=" + event.type());
        try {
            Notification notification = new Notification();
            notification.setUsername(event.username());
            notification.setType(NotificationType.fromString(event.type()));
            notification.setMessage(event.message());

            Notification saved = notificationCommandPort.save(notification);
            logger.info("NOTIF_EVENT PERSISTED id=" + Slf4jLoggingAdapter.shortId(saved.getId())
                    + " username=" + event.username()
                    + " type=" + event.type());

            push.sendToUser(event.username(), "/queue/notifications", event);
            logger.info("NOTIF_EVENT PUSH_SENT username=" + event.username() + " destination=/queue/notifications");
        } catch (Exception e) {
            logger.error("NOTIF_EVENT FAILED username=" + event.username() + " type=" + event.type() + " reason=" + e.getMessage(), e);
        }
    }

    @Override
    public void onPasswordReset(PasswordResetEvent event) {
        String emailHash = Slf4jLoggingAdapter.hashEmail(event.email());
        logger.info("EMAIL_RESET START emailHash=" + emailHash);
        try {
            String link = frontendUrl + "/reset-password?token=" + event.token();
            emailPort.sendEmail(event.email(), "Password reset link", "Your password reset link is: " + link);
            logger.info("EMAIL_RESET SUCCESS emailHash=" + emailHash);
        } catch (Exception e) {
            logger.error("EMAIL_RESET FAILED emailHash=" + emailHash + " reason=" + e.getMessage(), e);
        }
    }

    @Override
    public void onUserRegistered(UserRegisteredEvent event) {
        String emailHash = Slf4jLoggingAdapter.hashEmail(event.email());
        logger.info("EMAIL_WELCOME START username=" + event.username() + " emailHash=" + emailHash);
        try {
            String body = "Your username: " + event.username()
                    + " your password: " + "[REDACTED]"
                    + " service url: " + frontendUrl;
            emailPort.sendEmail(event.email(), "Your password", body);
            logger.info("EMAIL_WELCOME SUCCESS username=" + event.username() + " emailHash=" + emailHash);
        } catch (Exception e) {
            logger.error("EMAIL_WELCOME FAILED username=" + event.username() + " emailHash=" + emailHash + " reason=" + e.getMessage(), e);
        }
    }
}