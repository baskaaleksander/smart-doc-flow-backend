package com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.port;

public interface ReviewDomainEventPublisherPort {
    void publish(Object domainEvent);
}
