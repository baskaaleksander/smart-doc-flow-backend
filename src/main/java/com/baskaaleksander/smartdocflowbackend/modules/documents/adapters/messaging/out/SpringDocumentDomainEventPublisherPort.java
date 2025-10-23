package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.messaging.out;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.DocumentDomainEventPublisherPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringDocumentDomainEventPublisherPort implements DocumentDomainEventPublisherPort {
    private final ApplicationEventPublisher publisher;

    public SpringDocumentDomainEventPublisherPort(
            ApplicationEventPublisher publisher
    ) {
        this.publisher = publisher;
    }
    @Override
    public void publish(Object domainEvent) {
        publisher.publishEvent(domainEvent);
    }
}
