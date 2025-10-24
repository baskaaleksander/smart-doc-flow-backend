package com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.messaging.out;

import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.port.RealtimePushPort;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketPushAdapter implements RealtimePushPort {

    private final SimpMessagingTemplate simpMessagingTemplate;

    @Override
    public void sendToUser(String user, String destination, Object payload) {
        simpMessagingTemplate.convertAndSendToUser(
                user,
                destination,
                payload
        );
    }
}
