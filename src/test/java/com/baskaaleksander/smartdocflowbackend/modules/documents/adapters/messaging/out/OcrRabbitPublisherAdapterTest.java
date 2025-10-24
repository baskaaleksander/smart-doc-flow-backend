package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.messaging.out;

import com.baskaaleksander.smartdocflowbackend.common.config.QueueConfig;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.event.OcrTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OcrRabbitPublisherAdapterTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OcrRabbitPublisherAdapter adapter;

    @Captor
    private ArgumentCaptor<MessagePostProcessor> mppCaptor;

    @Test
    void publish_sendsMessageWithDocumentIdHeader() {
        UUID docId = UUID.randomUUID();
        OcrTask task = new OcrTask(docId);

        adapter.publish(task);

        verify(rabbitTemplate).convertAndSend(
                eq(QueueConfig.EXCHANGE),
                eq(QueueConfig.OCR_ROUTING_KEY),
                eq(task),
                mppCaptor.capture()
        );

        MessageProperties props = new MessageProperties();
        Message msg = new Message(new byte[0], props);
        Message processed = mppCaptor.getValue().postProcessMessage(msg);

        Object headerValue = processed.getMessageProperties().getHeader("documentId");
        assertThat((String) headerValue).isEqualTo(docId.toString());
    }
}