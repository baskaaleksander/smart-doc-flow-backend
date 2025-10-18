package com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.messaging.in;

import com.baskaaleksander.smartdocflowbackend.modules.contracts.NotificationEvent;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.port.NotificationEventIngressPort;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;


@Component
public class SpringEventIngressAdapter {

    private final NotificationEventIngressPort ingress;

    public SpringEventIngressAdapter(NotificationEventIngressPort ingress) {
        this.ingress = ingress;
    }

    @EventListener
    public void handle(NotificationEvent event) {
        ingress.onNotificationSent(event);
    }
}
