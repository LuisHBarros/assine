package br.com.assine.fiscal.adapter.out.persistence;

import br.com.assine.fiscal.domain.model.Invoice;
import br.com.assine.fiscal.domain.port.out.InvoiceRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class InvoicePersistenceAdapter implements InvoiceRepository {

    private final InvoiceJpaRepository repository;

    public InvoicePersistenceAdapter(InvoiceJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Invoice save(Invoice invoice) {
        InvoiceEntity entity = toEntity(invoice);
        InvoiceEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Invoice> findByPaymentId(UUID paymentId) {
        return repository.findByPaymentId(paymentId).map(this::toDomain);
    }

    private InvoiceEntity toEntity(Invoice domain) {
        InvoiceEntity entity = new InvoiceEntity();
        entity.setId(domain.id());
        entity.setPaymentId(domain.paymentId());
        entity.setSubscriptionId(domain.subscriptionId());
        entity.setUserId(domain.userId());
        entity.setExternalId(domain.externalId());
        entity.setSeries(domain.series());
        entity.setNumber(domain.number());
        entity.setAmountCents(domain.amountCents());
        entity.setStatus(domain.status());
        entity.setIssuerResponse(domain.issuerResponse());
        entity.setPdfUrl(domain.pdfUrl());
        entity.setIssuedAt(domain.issuedAt());
        entity.setCreatedAt(domain.createdAt());
        return entity;
    }

    private Invoice toDomain(InvoiceEntity entity) {
        return new Invoice(
            entity.getId(),
            entity.getPaymentId(),
            entity.getSubscriptionId(),
            entity.getUserId(),
            entity.getExternalId(),
            entity.getSeries(),
            entity.getNumber(),
            entity.getAmountCents(),
            entity.getStatus(),
            entity.getIssuerResponse(),
            entity.getPdfUrl(),
            entity.getIssuedAt(),
            entity.getCreatedAt()
        );
    }
}
