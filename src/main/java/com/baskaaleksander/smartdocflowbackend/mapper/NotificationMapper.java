package com.baskaaleksander.smartdocflowbackend.mapper;

import com.baskaaleksander.smartdocflowbackend.dto.response.NotificationResponse;
import com.baskaaleksander.smartdocflowbackend.model.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponse toNotificationResponse(Notification notification);
}
