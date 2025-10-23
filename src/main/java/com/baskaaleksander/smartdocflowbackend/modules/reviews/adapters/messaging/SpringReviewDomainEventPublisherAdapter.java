package com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.messaging;

import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.port.ReviewDomainEventPublisherPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringReviewDomainEventPublisherAdapter implements ReviewDomainEventPublisherPort {

    private final ApplicationEventPublisher publisher;

    public SpringReviewDomainEventPublisherAdapter(
            ApplicationEventPublisher publisher
    ) {
        this.publisher = publisher;
    }
    @Override
    public void publish(Object domainEvent) {
        publisher.publishEvent(domainEvent);
    }
}
