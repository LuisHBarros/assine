package br.com.assine.fiscal.domain.port.in;

import java.util.UUID;

public interface IssueInvoiceUseCase {
    void handleConfirmedPayment(UUID paymentId, UUID subscriptionId, UUID userId, Integer amountCents);
}
