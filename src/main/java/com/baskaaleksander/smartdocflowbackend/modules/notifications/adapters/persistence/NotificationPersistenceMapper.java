package com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.persistence;

import com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.persistence.entity.NotificationEntity;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.model.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationPersistenceMapper {

        public Notification toDomain(NotificationEntity e) {
            Notification n = new Notification();
            n.setId(e.getId());
            n.setUsername(e.getUsername());
            n.setType(e.getType());
            n.setMessage(e.getMessage());
            n.setRead(e.isRead());
            n.setCreatedAt(e.getCreatedAt());

            return n;
        }
    }

