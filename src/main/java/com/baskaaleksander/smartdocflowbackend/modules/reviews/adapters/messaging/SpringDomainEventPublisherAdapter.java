package com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.messaging;

import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.port.DomainEventPublisherPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDomainEventPublisherAdapter implements DomainEventPublisherPort {

    private final ApplicationEventPublisher publisher;

    public SpringDomainEventPublisherAdapter(
            ApplicationEventPublisher publisher
    ) {
        this.publisher = publisher;
    }
    @Override
    public void publish(Object domainEvent) {
        publisher.publishEvent(domainEvent);
    }
}
