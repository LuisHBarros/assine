package br.com.assine.fiscal.domain.port.out;

import br.com.assine.fiscal.domain.model.Invoice;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository {
    Invoice save(Invoice invoice);
    Optional<Invoice> findByPaymentId(UUID paymentId);
}
