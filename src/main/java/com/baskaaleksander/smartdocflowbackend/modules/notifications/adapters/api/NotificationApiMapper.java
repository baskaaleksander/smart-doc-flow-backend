package com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.api;

import com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.api.dto.NotificationResponse;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.model.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationApiMapper {

    NotificationResponse toResponse(Notification notification);
}
