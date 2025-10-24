package com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.messaging.out;

import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.port.EmailSenderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailSenderAdapter implements EmailSenderPort {

    private final JavaMailSender mailSender;

    @Override
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            //TODO: change this
            System.out.println(e.getMessage());
        }
    }
}
