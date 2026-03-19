package br.com.assine.fiscal.application.usecase;

import br.com.assine.fiscal.domain.event.InvoiceFailed;
import br.com.assine.fiscal.domain.event.InvoiceIssued;
import br.com.assine.fiscal.domain.model.Invoice;
import br.com.assine.fiscal.domain.model.InvoiceOutbox;
import br.com.assine.fiscal.domain.model.InvoiceStatus;
import br.com.assine.fiscal.domain.port.out.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InvoiceRetryJob {

    private static final Logger log = LoggerFactory.getLogger(InvoiceRetryJob.class);

    private final InvoiceOutboxRepository outboxRepository;
    private final InvoiceRepository invoiceRepository;
    private final SubscriptionGateway subscriptionGateway;
    private final FiscalGateway fiscalGateway;
    private final InvoiceStorageGateway storageGateway;
    private final DomainEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Value("${invoice.retry.max-attempts:5}")
    private int maxAttempts;

    @Value("${invoice.retry.backoff-initial:1000}")
    private long backoffInitial;

    @Value("${invoice.retry.backoff-multiplier:2}")
    private int backoffMultiplier;

    public InvoiceRetryJob(
        InvoiceOutboxRepository outboxRepository,
        InvoiceRepository invoiceRepository,
        SubscriptionGateway subscriptionGateway,
        FiscalGateway fiscalGateway,
        InvoiceStorageGateway storageGateway,
        DomainEventPublisher eventPublisher,
        ObjectMapper objectMapper
    ) {
        this.outboxRepository = outboxRepository;
        this.invoiceRepository = invoiceRepository;
        this.subscriptionGateway = subscriptionGateway;
        this.fiscalGateway = fiscalGateway;
        this.storageGateway = storageGateway;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${invoice.retry.interval:5000}")
    public void processOutbox() {
        List<InvoiceOutbox> records = outboxRepository.findUnissued(10);
        if (records.isEmpty()) {
            return;
        }

        log.info("Processing {} unissued invoice outbox records", records.size());
        for (InvoiceOutbox record : records) {
            if (shouldRetry(record)) {
                processRecord(record);
            }
        }
    }

    private boolean shouldRetry(InvoiceOutbox record) {
        if (record.attempts() == 0) return true;

        long delay = (long) (backoffInitial * Math.pow(backoffMultiplier, record.attempts() - 1));
        LocalDateTime nextRetry = record.lastAttemptAt().plusNanos(delay * 1_000_000);
        return LocalDateTime.now().isAfter(nextRetry);
    }

    private void processRecord(InvoiceOutbox record) {
        log.info("Processing invoice for paymentId: {}. Attempt: {}", record.paymentId(), record.attempts() + 1);

        try {
            Map<String, Object> payload = objectMapper.readValue(record.payload(), Map.class);
            UUID subscriptionId = UUID.fromString((String) payload.get("subscriptionId"));
            UUID userId = UUID.fromString((String) payload.get("userId"));
            Integer amountCents = (Integer) payload.get("amountCents");

            // 1. Fetch Payer Data
            FiscalGateway.PayerData payer = subscriptionGateway.fetchPayerData(subscriptionId);

            // 2. Issue Invoice via Fiscal Gateway
            FiscalGateway.FiscalResponse response = fiscalGateway.issue(payer, amountCents, record.paymentId());

            // 3. Store PDF
            Invoice invoice = new Invoice(
                UUID.randomUUID(),
                record.paymentId(),
                subscriptionId,
                userId,
                response.externalId(),
                response.series(),
                response.number(),
                amountCents,
                InvoiceStatus.ISSUED,
                response.rawResponse(),
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
            );

            String pdfUrl = storageGateway.store(invoice.id(), response.pdfContent());

            // 4. Update and Save
            Invoice updatedInvoice = new Invoice(
                invoice.id(),
                invoice.paymentId(),
                invoice.subscriptionId(),
                invoice.userId(),
                invoice.externalId(),
                invoice.series(),
                invoice.number(),
                invoice.amountCents(),
                invoice.status(),
                invoice.issuerResponse(),
                pdfUrl,
                invoice.issuedAt(),
                invoice.createdAt()
            );

            invoiceRepository.save(updatedInvoice);

            // 5. Mark Outbox as Issued
            InvoiceOutbox updatedRecord = new InvoiceOutbox(
                record.id(),
                record.paymentId(),
                record.payload(),
                record.attempts() + 1,
                LocalDateTime.now(),
                true,
                record.createdAt()
            );
            outboxRepository.save(updatedRecord);

            // 6. Publish Success Event
            eventPublisher.publishIssued(new InvoiceIssued(
                updatedInvoice.id(),
                updatedInvoice.paymentId(),
                updatedInvoice.userId(),
                updatedInvoice.externalId(),
                updatedInvoice.pdfUrl()
            ));

            log.info("Invoice issued successfully for paymentId: {}", record.paymentId());

        } catch (Exception e) {
            log.error("Failed to issue invoice for paymentId: {}. Error: {}", record.paymentId(), e.getMessage());

            int newAttempts = record.attempts() + 1;
            boolean failedPermanently = newAttempts >= maxAttempts;

            InvoiceOutbox updatedRecord = new InvoiceOutbox(
                record.id(),
                record.paymentId(),
                record.payload(),
                newAttempts,
                LocalDateTime.now(),
                failedPermanently, // if failed permanently, mark as "issued" (processed) to stop retrying
                record.createdAt()
            );
            outboxRepository.save(updatedRecord);

            if (failedPermanently) {
                log.error("Invoice issuance failed permanently after {} attempts for paymentId: {}", maxAttempts, record.paymentId());

                // Create a FAILED invoice record
                try {
                    Map<String, Object> payload = objectMapper.readValue(record.payload(), Map.class);
                    UUID subscriptionId = UUID.fromString((String) payload.get("subscriptionId"));
                    UUID userId = UUID.fromString((String) payload.get("userId"));
                    Integer amountCents = (Integer) payload.get("amountCents");

                    Invoice failedInvoice = new Invoice(
                        UUID.randomUUID(),
                        record.paymentId(),
                        subscriptionId,
                        userId,
                        null,
                        null,
                        null,
                        amountCents,
                        InvoiceStatus.FAILED,
                        e.getMessage(),
                        null,
                        null,
                        LocalDateTime.now()
                    );
                    invoiceRepository.save(failedInvoice);

                    eventPublisher.publishFailed(new InvoiceFailed(
                        record.paymentId(),
                        userId,
                        e.getMessage()
                    ));
                } catch (Exception innerEx) {
                    log.error("Critical: Could not even save failed invoice record", innerEx);
                }
            }
        }
    }
}
