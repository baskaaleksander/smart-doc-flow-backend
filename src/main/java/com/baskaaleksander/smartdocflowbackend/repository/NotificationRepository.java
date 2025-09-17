package com.baskaaleksander.smartdocflowbackend.repository;

import com.baskaaleksander.smartdocflowbackend.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findAllByUsernameAndRead(Pageable pageable, String username, boolean read);

    Page<Notification> findAllByUsername(Pageable pageable, String username);

    Integer getNotificationsCountByUsernameAndRead(String username, boolean read);

    @Query("update Notification n set n.read = true where n.username = :username and n.id = :id and n.read = false")
    @Modifying
    Integer markOneRead(String username, UUID id);

    @Query("update Notification n set n.read = true where n.username = :username and n.read = false")
    @Modifying
    Integer markAllRead(String username);
}
