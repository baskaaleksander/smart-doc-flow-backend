package com.baskaaleksander.smartdocflowbackend.repository;

import com.baskaaleksander.smartdocflowbackend.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findAllByUsernameAndRead(Pageable pageable, String username, boolean read);

    Page<Notification> findAllByUsername(Pageable pageable, String username);

    Integer getNotificationsCountByUsernameAndRead(String username, boolean read);
}
