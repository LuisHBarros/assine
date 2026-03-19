package br.com.assine.fiscal.domain.event;

import java.util.UUID;

public record InvoiceFailed(
    UUID paymentId,
    UUID userId,
    String reason
) {}
