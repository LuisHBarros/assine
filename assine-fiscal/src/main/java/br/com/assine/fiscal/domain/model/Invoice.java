package br.com.assine.fiscal.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record Invoice(
    UUID id,
    UUID paymentId,
    UUID subscriptionId,
    UUID userId,
    String externalId,
    String series,
    String number,
    Integer amountCents,
    InvoiceStatus status,
    String issuerResponse,
    String pdfUrl,
    LocalDateTime issuedAt,
    LocalDateTime createdAt
) {
    public static Invoice newInvoice(UUID paymentId, UUID subscriptionId, UUID userId, Integer amountCents) {
        return new Invoice(
            UUID.randomUUID(),
            paymentId,
            subscriptionId,
            userId,
            null,
            null,
            null,
            amountCents,
            InvoiceStatus.PENDING,
            null,
            null,
            null,
            LocalDateTime.now()
        );
    }
}
