package com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.messaging.in;

import com.baskaaleksander.smartdocflowbackend.modules.contracts.NotificationEvent;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.port.NotificationEventIngressPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpringEventIngressAdapterTest {

    @Mock
    private NotificationEventIngressPort ingress;

    @InjectMocks
    private SpringEventIngressAdapter adapter;

    @Test
    void handle_forwardsEventToIngressPort() {
        NotificationEvent event = new NotificationEvent("john", "type1", "message");

        adapter.handle(event);

        verify(ingress).onNotificationSent(event);
    }
}