package com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.messaging.out;

import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.port.EmailSenderPort;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailSenderAdapter implements EmailSenderPort {

    private final JavaMailSender mailSender;

    public EmailSenderAdapter(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    @Override
    public void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
