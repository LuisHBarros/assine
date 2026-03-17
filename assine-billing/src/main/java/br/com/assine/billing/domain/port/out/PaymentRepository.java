package br.com.assine.billing.domain.port.out;

import br.com.assine.billing.domain.model.IdempotencyKey;
import br.com.assine.billing.domain.model.Payment;
import java.util.Optional;

public interface PaymentRepository {
    Optional<Payment> findByIdempotencyKey(IdempotencyKey idempotencyKey);
    Optional<Payment> findByExternalId(String externalId);
    Payment save(Payment payment);
}
