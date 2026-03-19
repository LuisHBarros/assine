package br.com.assine.fiscal.application.usecase;

import br.com.assine.fiscal.domain.model.InvoiceOutbox;
import br.com.assine.fiscal.domain.port.in.IssueInvoiceUseCase;
import br.com.assine.fiscal.domain.port.out.InvoiceOutboxRepository;
import br.com.assine.fiscal.domain.port.out.InvoiceRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IssueInvoiceService implements IssueInvoiceUseCase {

    private static final Logger log = LoggerFactory.getLogger(IssueInvoiceService.class);

    private final InvoiceRepository invoiceRepository;
    private final InvoiceOutboxRepository outboxRepository;

    public IssueInvoiceService(InvoiceRepository invoiceRepository, InvoiceOutboxRepository outboxRepository) {
        this.invoiceRepository = invoiceRepository;
        this.outboxRepository = outboxRepository;
    }

    @Override
    @Transactional
    public void handleConfirmedPayment(UUID paymentId, UUID subscriptionId, UUID userId, Integer amountCents) {
        log.info("Handling confirmed payment for paymentId: {}", paymentId);

        if (invoiceRepository.findByPaymentId(paymentId).isPresent()) {
            log.info("Invoice already exists for paymentId: {}. Skipping.", paymentId);
            return;
        }

        if (outboxRepository.findByPaymentId(paymentId).isPresent()) {
            log.info("Invoice outbox record already exists for paymentId: {}. Skipping.", paymentId);
            return;
        }

        // Payload for the outbox to reconstruct the needed data
        String payload = String.format("{\"subscriptionId\":\"%s\", \"userId\":\"%s\", \"amountCents\":%d}",
            subscriptionId, userId, amountCents);

        InvoiceOutbox outbox = InvoiceOutbox.newRecord(paymentId, payload);
        outboxRepository.save(outbox);

        log.info("Created outbox record for invoice issuance. paymentId: {}", paymentId);
    }
}
