package com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.port;

public interface EmailSenderPort {
    void sendEmail(String to, String subject, String body);
}
