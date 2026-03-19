package br.com.assine.fiscal.adapter.out.persistence;

import br.com.assine.fiscal.domain.model.InvoiceOutbox;
import br.com.assine.fiscal.domain.port.out.InvoiceOutboxRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class InvoiceOutboxPersistenceAdapter implements InvoiceOutboxRepository {

    private final InvoiceOutboxJpaRepository repository;

    public InvoiceOutboxPersistenceAdapter(InvoiceOutboxJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public InvoiceOutbox save(InvoiceOutbox domain) {
        InvoiceOutboxEntity entity = toEntity(domain);
        InvoiceOutboxEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<InvoiceOutbox> findByPaymentId(UUID paymentId) {
        return repository.findByPaymentId(paymentId).map(this::toDomain);
    }

    @Override
    public List<InvoiceOutbox> findUnissued(int limit) {
        return repository.findUnissued(PageRequest.of(0, limit))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    private InvoiceOutboxEntity toEntity(InvoiceOutbox domain) {
        InvoiceOutboxEntity entity = new InvoiceOutboxEntity();
        entity.setId(domain.id());
        entity.setPaymentId(domain.paymentId());
        entity.setPayload(domain.payload());
        entity.setAttempts(domain.attempts());
        entity.setLastAttemptAt(domain.lastAttemptAt());
        entity.setIssued(domain.issued());
        entity.setCreatedAt(domain.createdAt());
        return entity;
    }

    private InvoiceOutbox toDomain(InvoiceOutboxEntity entity) {
        return new InvoiceOutbox(
            entity.getId(),
            entity.getPaymentId(),
            entity.getPayload(),
            entity.getAttempts(),
            entity.getLastAttemptAt(),
            entity.getIssued(),
            entity.getCreatedAt()
        );
    }
}
