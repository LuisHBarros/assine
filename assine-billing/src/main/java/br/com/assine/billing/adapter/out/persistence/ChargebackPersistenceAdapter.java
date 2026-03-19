package br.com.assine.billing.adapter.out.persistence;

import br.com.assine.billing.domain.model.Chargeback;
import br.com.assine.billing.domain.model.ChargebackStatus;
import br.com.assine.billing.domain.model.PaymentId;
import br.com.assine.billing.domain.port.out.ChargebackRepository;
import org.springframework.stereotype.Component;

@Component
public class ChargebackPersistenceAdapter implements ChargebackRepository {

    private final ChargebackJpaRepository chargebackJpaRepository;

    public ChargebackPersistenceAdapter(ChargebackJpaRepository chargebackJpaRepository) {
        this.chargebackJpaRepository = chargebackJpaRepository;
    }

    @Override
    public Chargeback save(Chargeback chargeback) {
        ChargebackEntity entity = toEntity(chargeback);
        ChargebackEntity saved = chargebackJpaRepository.save(entity);
        return toDomain(saved);
    }

    private Chargeback toDomain(ChargebackEntity entity) {
        return new Chargeback(
                entity.getId(),
                new PaymentId(entity.getPaymentId()),
                entity.getSubscriptionId(),
                entity.getExternalId(),
                entity.getAmountCents(),
                ChargebackStatus.valueOf(entity.getStatus()),
                entity.getOpenedAt(),
                entity.getResolvedAt()
        );
    }

    private ChargebackEntity toEntity(Chargeback chargeback) {
        ChargebackEntity entity = new ChargebackEntity();
        if (chargeback.getId() != null) {
            entity.setId(chargeback.getId());
        }
        entity.setPaymentId(chargeback.getPaymentId().value());
        entity.setSubscriptionId(chargeback.getSubscriptionId());
        entity.setExternalId(chargeback.getExternalId());
        entity.setAmountCents(chargeback.getAmountCents());
        entity.setStatus(chargeback.getStatus().name());
        entity.setOpenedAt(chargeback.getOpenedAt());
        entity.setResolvedAt(chargeback.getResolvedAt());
        return entity;
    }
}
