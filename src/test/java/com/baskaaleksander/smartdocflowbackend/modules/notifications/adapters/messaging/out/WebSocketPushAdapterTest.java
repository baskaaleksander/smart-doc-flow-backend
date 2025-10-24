package com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.messaging.out;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebSocketPushAdapterTest {

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    @InjectMocks
    private WebSocketPushAdapter adapter;

    @Test
    void sendToUser_forwardsToSimpMessagingTemplate() {
        String user = "john";
        String destination = "/queue/notifications";
        Object payload = new Object();

        adapter.sendToUser(user, destination, payload);

        verify(simpMessagingTemplate).convertAndSendToUser(user, destination, payload);
    }
}