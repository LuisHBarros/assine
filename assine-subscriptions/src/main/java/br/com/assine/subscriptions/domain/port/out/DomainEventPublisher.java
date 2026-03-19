package br.com.assine.subscriptions.domain.port.out;

import br.com.assine.subscriptions.domain.event.DomainEvent;

public interface DomainEventPublisher {
    void publish(DomainEvent event);
}
