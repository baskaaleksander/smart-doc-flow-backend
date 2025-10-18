package com.baskaaleksander.smartdocflowbackend.modules.notifications.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    Page<NotificationEntity> findAllByUsernameAndRead(Pageable pageable, String username, boolean read);

    Page<NotificationEntity> findAllByUsername(Pageable pageable, String username);

    @Query("select count(n) from NotificationEntity n where n.username = :username and n.read = :read")
    Integer getNotificationsCountByUsernameAndRead(String username, boolean read);

    @Query("update NotificationEntity n set n.read = true where n.username = :username and n.id = :id and n.read = false")
    @Modifying
    Integer markOneRead(String username, UUID id);

    @Query("update NotificationEntity n set n.read = true where n.username = :username and n.read = false")
    @Modifying
    Integer markAllRead(String username);
}
