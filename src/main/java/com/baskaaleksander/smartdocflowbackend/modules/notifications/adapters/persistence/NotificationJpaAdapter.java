package com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.persistence;

import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationUtil;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.persistence.entity.NotificationEntity;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.persistence.spring.SpringDataNotificationRepository;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.model.Notification;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.port.NotificationCommandPort;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.port.NotificationQueryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    @Transactional
    public Notification save(Notification notification) {
        NotificationEntity entity = (notification.getId() != null) ?
                notificationRepo.findById(notification.getId()).orElseGet(NotificationEntity::new) :
                new NotificationEntity();

        entity.setUsername(notification.getUsername());
        entity.setType(notification.getType());
        entity.setMessage(notification.getMessage());
        entity.setRead(notification.isRead());

        NotificationEntity saved = notificationRepo.save(entity);

        return mapper.toDomain(saved);
    }

    @Override
    @Transactional
    public Integer markOneRead(String username, UUID id) {
        return notificationRepo.markOneRead(username, id);
    }

    @Override
    @Transactional
    public Integer markAllRead(String username) {
        return notificationRepo.markAllRead(username);
    }

    @Override
    public PagingResult<Notification> findAllByUsernameAndRead(PaginationRequest request, String username, boolean read) {
        Pageable pageable = PaginationUtil.getPageable(request);
        Page<NotificationEntity> entities = notificationRepo.findAllByUsernameAndRead(pageable, username, read);

        List<Notification> content = entities.getContent().stream().map(mapper::toDomain).toList();

        for (Notification c : content) {
            System.out.println(c.getMessage());
        }

        return new PagingResult<>(
                content,
                entities.getTotalPages(),
                entities.getTotalElements(),
                entities.getSize(),
                entities.getNumber(),
                entities.isLast(),
                entities.hasNext()
        );
    }

    @Override
    public PagingResult<Notification> findAllByUsername(PaginationRequest request, String username) {
        Pageable pageable = PaginationUtil.getPageable(request);
        Page<NotificationEntity> entities = notificationRepo.findAllByUsername(pageable, username);

        List<Notification> content = entities.getContent().stream().map(mapper::toDomain).toList();

        return new PagingResult<>(
                content,
                entities.getTotalPages(),
                entities.getTotalElements(),
                entities.getSize(),
                entities.getNumber(),
                entities.isLast(),
                entities.hasNext()
        );
    }

    @Override
    public Integer getNotificationsCountByUsernameAndRead(String username, boolean read) {
        return notificationRepo.getNotificationsCountByUsernameAndRead(username, read);
    }
}
