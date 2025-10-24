package com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.messaging.in;

import com.baskaaleksander.smartdocflowbackend.modules.contracts.NotificationEvent;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.port.NotificationEventIngressPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class SpringEventIngressAdapter {

    private final NotificationEventIngressPort ingress;

    @EventListener
    public void handle(NotificationEvent event) {
        ingress.onNotificationSent(event);
    }
}
