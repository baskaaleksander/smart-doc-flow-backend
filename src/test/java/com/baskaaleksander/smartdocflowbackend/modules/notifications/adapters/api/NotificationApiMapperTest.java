package com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.api;

import com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.api.dto.NotificationResponse;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.model.Notification;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.model.NotificationType;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationApiMapperTest {

    private final NotificationApiMapper mapper = Mappers.getMapper(NotificationApiMapper.class);

    @Test
    void toResponse_mapsAllFieldsCorrectly() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.now();

        Notification notification = new Notification();
        notification.setId(id);
        notification.setUsername("john");
        notification.setType(NotificationType.REVIEW_COMMENT);
        notification.setMessage("Document reviewed");
        notification.setRead(false);
        notification.setCreatedAt(createdAt);

        NotificationResponse dto = mapper.toResponse(notification);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.username()).isEqualTo("john");
        assertThat(dto.type()).isEqualTo(NotificationType.REVIEW_COMMENT);
        assertThat(dto.message()).isEqualTo("Document reviewed");
        assertThat(dto.read()).isFalse();
        assertThat(dto.createdAt()).isEqualTo(createdAt);
    }
}