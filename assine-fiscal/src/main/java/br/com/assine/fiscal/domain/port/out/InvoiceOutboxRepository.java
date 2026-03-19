package br.com.assine.fiscal.domain.port.out;

import br.com.assine.fiscal.domain.model.InvoiceOutbox;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceOutboxRepository {
    InvoiceOutbox save(InvoiceOutbox record);
    Optional<InvoiceOutbox> findByPaymentId(UUID paymentId);
    List<InvoiceOutbox> findUnissued(int limit);
}
