package com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.messaging.in;

import com.baskaaleksander.smartdocflowbackend.modules.contracts.NotificationEvent;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.port.NotificationEventIngressPort;
import org.springframework.stereotype.Service;


@Service
public class SpringEventIngressAdapter implements NotificationEventIngressPort {


    @Override
    public void onNotificationSent(NotificationEvent event) {

    }
}
