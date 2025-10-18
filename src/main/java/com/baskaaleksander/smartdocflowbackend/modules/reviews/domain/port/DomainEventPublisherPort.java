package com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.port;

public interface DomainEventPublisherPort {
    void publish(Object domainEvent);
}
