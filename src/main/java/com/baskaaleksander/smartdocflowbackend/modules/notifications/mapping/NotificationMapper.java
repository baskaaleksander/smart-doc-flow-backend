package com.baskaaleksander.smartdocflowbackend.modules.notifications.mapping;

import com.baskaaleksander.smartdocflowbackend.modules.notifications.api.dto.NotificationResponse;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.persistence.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponse toNotificationResponse(Notification notification);
}
