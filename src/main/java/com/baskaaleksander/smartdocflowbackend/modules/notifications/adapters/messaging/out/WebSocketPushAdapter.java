package com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.messaging.out;

import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.port.RealtimePushPort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class WebSocketPushAdapter implements RealtimePushPort {

    private final SimpMessagingTemplate simpMessagingTemplate;

    public WebSocketPushAdapter(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }
    @Override
    public void sendToUser(String user, String destination, Object payload) {
        simpMessagingTemplate.convertAndSendToUser(
                user,
                destination,
                payload
        );
    }
}
