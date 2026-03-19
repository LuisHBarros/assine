package br.com.assine.content.domain.port.out;

import br.com.assine.content.domain.event.ContentReadyEvent;

public interface EventPublisherPort {
    void publish(ContentReadyEvent event);
}
