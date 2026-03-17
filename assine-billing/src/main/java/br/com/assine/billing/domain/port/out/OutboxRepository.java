package br.com.assine.billing.domain.port.out;

import br.com.assine.billing.domain.event.DomainEvent;
import java.util.UUID;

public interface OutboxRepository {
    void save(DomainEvent event, UUID aggregateId);
}
