package com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.port;

import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.model.Notification;

public interface NotificationQueryPort {
    PagingResult<Notification> findAllByUsernameAndRead(PaginationRequest request, String username, boolean read);
    PagingResult<Notification> findAllByUsername(PaginationRequest request, String username);
    Integer getNotificationsCountByUsernameAndRead(String username, boolean read);
}
