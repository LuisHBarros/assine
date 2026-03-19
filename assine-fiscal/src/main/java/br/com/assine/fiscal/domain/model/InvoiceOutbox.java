package br.com.assine.fiscal.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record InvoiceOutbox(
    UUID id,
    UUID paymentId,
    String payload,
    Integer attempts,
    LocalDateTime lastAttemptAt,
    Boolean issued,
    LocalDateTime createdAt
) {
    public static InvoiceOutbox newRecord(UUID paymentId, String payload) {
        return new InvoiceOutbox(
            UUID.randomUUID(),
            paymentId,
            payload,
            0,
            null,
            false,
            LocalDateTime.now()
        );
    }
}
