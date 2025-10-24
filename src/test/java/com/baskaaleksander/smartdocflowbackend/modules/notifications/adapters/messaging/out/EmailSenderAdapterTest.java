package com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.messaging.out;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailSenderAdapterTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailSenderAdapter adapter;

    @Test
    void sendEmail_sendsMessage() {
        adapter.sendEmail("john@doe.com", "Subject", "Body text");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();

        assert msg.getTo() != null && msg.getTo()[0].equals("john@doe.com");
        assert msg.getSubject().equals("Subject");
        assert msg.getText().equals("Body text");
    }

    @Test
    void sendEmail_handlesExceptionGracefully() {
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));

        adapter.sendEmail("test@ex.com", "Err", "Msg");

        verify(mailSender).send(any(SimpleMailMessage.class));
        // no exception should propagate
    }
}