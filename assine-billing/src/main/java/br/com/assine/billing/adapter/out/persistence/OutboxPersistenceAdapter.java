package br.com.assine.billing.adapter.out.persistence;

import br.com.assine.billing.domain.event.DomainEvent;
import br.com.assine.billing.domain.port.out.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OutboxPersistenceAdapter implements OutboxRepository {

    private final OutboxJpaRepository outboxJpaRepository;
    private final ObjectMapper objectMapper;

    public OutboxPersistenceAdapter(OutboxJpaRepository outboxJpaRepository, ObjectMapper objectMapper) {
        this.outboxJpaRepository = outboxJpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(DomainEvent event, UUID aggregateId) {
        try {
            OutboxEventEntity entity = new OutboxEventEntity();
            entity.setAggregateId(aggregateId);
            entity.setEventType(event.getClass().getSimpleName());
            entity.setPayload(objectMapper.writeValueAsString(event));
            
            outboxJpaRepository.save(entity);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing domain event to JSON", e);
        }
    }
}
