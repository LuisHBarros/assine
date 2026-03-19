package br.com.assine.fiscal.domain.event;

import java.util.UUID;

public record InvoiceIssued(
    UUID invoiceId,
    UUID paymentId,
    UUID userId,
    String externalId,
    String pdfUrl
) {}
