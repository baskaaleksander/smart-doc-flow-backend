package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.messaging.out;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpringDocumentDomainEventPublisherPortTest {

    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private SpringDocumentDomainEventPublisherPort adapter;

    @Test
    void publish_delegatesToApplicationEventPublisher() {
        Object event = new Object();

        adapter.publish(event);

        verify(publisher).publishEvent(event);
    }
}