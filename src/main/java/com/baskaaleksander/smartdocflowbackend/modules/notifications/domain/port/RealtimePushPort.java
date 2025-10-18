package com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.port;

public interface RealtimePushPort {
    void sendToUser(String user, String destination, Object payload);
}
