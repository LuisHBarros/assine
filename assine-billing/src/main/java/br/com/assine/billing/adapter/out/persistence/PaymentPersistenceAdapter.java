package br.com.assine.billing.adapter.out.persistence;

import br.com.assine.billing.domain.model.*;
import br.com.assine.billing.domain.port.out.PaymentRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PaymentPersistenceAdapter implements PaymentRepository {

    private final PaymentJpaRepository paymentJpaRepository;

    public PaymentPersistenceAdapter(PaymentJpaRepository paymentJpaRepository) {
        this.paymentJpaRepository = paymentJpaRepository;
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(IdempotencyKey idempotencyKey) {
        return paymentJpaRepository.findByIdempotencyKey(idempotencyKey.value())
                .map(this::toDomain);
    }

    @Override
    public Optional<Payment> findByExternalId(String externalId) {
        return paymentJpaRepository.findByExternalId(externalId)
                .map(this::toDomain);
    }

    @Override
    public Payment save(Payment payment) {
        PaymentEntity entity = toEntity(payment);
        PaymentEntity saved = paymentJpaRepository.save(entity);
        return toDomain(saved);
    }

    private Payment toDomain(PaymentEntity entity) {
        return new Payment(
                new PaymentId(entity.getId()),
                entity.getSubscriptionId(),
                entity.getExternalId(),
                new IdempotencyKey(entity.getIdempotencyKey()),
                entity.getAmountCents(),
                PaymentMethod.valueOf(entity.getPaymentMethod()),
                PaymentStatus.valueOf(entity.getStatus()),
                entity.getConfirmedAt(),
                entity.getCreatedAt()
        );
    }

    private PaymentEntity toEntity(Payment payment) {
        PaymentEntity entity = new PaymentEntity();
        if (payment.getId() != null) {
            entity.setId(payment.getId().value());
        }
        entity.setSubscriptionId(payment.getSubscriptionId());
        entity.setExternalId(payment.getExternalId());
        entity.setIdempotencyKey(payment.getIdempotencyKey().value());
        entity.setAmountCents(payment.getAmountCents());
        entity.setPaymentMethod(payment.getPaymentMethod().name());
        entity.setStatus(payment.getStatus().name());
        entity.setConfirmedAt(payment.getConfirmedAt());
        entity.setCreatedAt(payment.getCreatedAt());
        return entity;
    }
}
