package com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.messaging.in;

import com.baskaaleksander.smartdocflowbackend.modules.contracts.PasswordResetEvent;
import com.baskaaleksander.smartdocflowbackend.modules.contracts.UserRegisteredEvent;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.port.AuthEventIngressPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitEventIngressAdapterTest {

    @Mock
    private AuthEventIngressPort ingress;

    @InjectMocks
    private RabbitEventIngressAdapter adapter;

    @Test
    void handleRegistered_forwardsEventToIngress() {
        UserRegisteredEvent event = new UserRegisteredEvent("john@doe.com", "john", "pwd123");

        adapter.handleRegistered(event);

        verify(ingress).onUserRegistered(event);
    }

    @Test
    void handleReset_forwardsEventToIngress() {
        PasswordResetEvent event = new PasswordResetEvent("kate@doe.com", "tokenXYZ");

        adapter.handleReset(event);

        verify(ingress).onPasswordReset(event);
    }
}