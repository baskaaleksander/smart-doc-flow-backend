package com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.persistence;

import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.persistence.spring.SpringDataNotificationRepository;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.model.Notification;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.port.NotificationCommandPort;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.port.NotificationQueryPort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class NotificationJpaAdapter implements NotificationCommandPort, NotificationQueryPort {
    private final SpringDataNotificationRepository notificationRepo;
    private final NotificationPersistenceMapper mapper;

    public NotificationJpaAdapter(
            SpringDataNotificationRepository notificationRepo,
            NotificationPersistenceMapper mapper
    ) {
        this.notificationRepo = notificationRepo;
        this.mapper = mapper;
    }
    @Override
    public Notification save(Notification notification) {
        return null;
    }

    @Override
    public Integer markOneRead(String username, UUID id) {
        return 0;
    }

    @Override
    public Integer markAllRead(String username) {
        return 0;
    }

    @Override
    public PagingResult<Notification> findAllByUsernameAndRead(PaginationRequest request, String username, boolean read) {
        return null;
    }

    @Override
    public PagingResult<Notification> findAllByUsername(PaginationRequest request, String username) {
        return null;
    }

    @Override
    public Integer getNotificationsCountByUsernameAndRead(String username, boolean read) {
        return 0;
    }
}
