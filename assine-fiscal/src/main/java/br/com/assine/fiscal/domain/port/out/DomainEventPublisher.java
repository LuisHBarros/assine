package br.com.assine.fiscal.domain.port.out;

import br.com.assine.fiscal.domain.event.InvoiceFailed;
import br.com.assine.fiscal.domain.event.InvoiceIssued;

public interface DomainEventPublisher {
    void publishIssued(InvoiceIssued event);
    void publishFailed(InvoiceFailed event);
}
