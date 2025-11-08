package com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.messaging.out;

import com.baskaaleksander.smartdocflowbackend.common.logging.LoggingPort;
import com.baskaaleksander.smartdocflowbackend.common.logging.Slf4jLoggingAdapter;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.port.EmailSenderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailSenderAdapter implements EmailSenderPort {

    private final JavaMailSender mailSender;
    private final LoggingPort logger;

    @Value("${spring.mail.address}")
    private String emailAddress;

    @Override
    public void sendEmail(String to, String subject, String body) {
        String emailHash = Slf4jLoggingAdapter.hashEmail(to);
        logger.info("EMAIL_SEND START emailHash=" + emailHash + " subject=" + subject);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            logger.info("EMAIL_SEND SUCCESS emailHash=" + emailHash + " subject=" + subject);
        } catch (Exception e) {
            logger.error("EMAIL_SEND FAILED emailHash=" + emailHash + " reason=" + e.getMessage(), e);
        }
    }
}