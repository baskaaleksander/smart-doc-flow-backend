package com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.persistence.spring;

import com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.persistence.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SpringDataNotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    Page<NotificationEntity> findAllByUsernameAndRead(String username, boolean read, Pageable pageable);

    Page<NotificationEntity> findAllByUsername(String username, Pageable pageable);

    @Query("select count(n) from NotificationEntity n where n.username = :username and n.read = :read")
    Integer getNotificationsCountByUsernameAndRead(String username, boolean read);

    @Query("update NotificationEntity n set n.read = true where n.username = :username and n.id = :id and n.read = false")
    @Modifying
    Integer markOneRead(String username, UUID id);

    @Query("update NotificationEntity n set n.read = true where n.username = :username and n.read = false")
    @Modifying
    Integer markAllRead(String username);
}
