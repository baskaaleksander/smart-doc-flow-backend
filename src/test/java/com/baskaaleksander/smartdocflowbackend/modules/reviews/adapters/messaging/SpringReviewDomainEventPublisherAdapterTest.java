package com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpringReviewDomainEventPublisherAdapterTest {

    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private SpringReviewDomainEventPublisherAdapter adapter;

    @Test
    void publish_forwardsEventToSpringPublisher() {
        Object event = new Object();

        adapter.publish(event);

        verify(publisher).publishEvent(event);
    }
}