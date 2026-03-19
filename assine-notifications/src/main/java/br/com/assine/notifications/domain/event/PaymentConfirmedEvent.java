package br.com.assine.notifications.domain.event;

import java.util.UUID;

public record PaymentConfirmedEvent(
    UUID paymentId,
    UUID subscriptionId,
    String userEmail,
    String userName,
    String amount,
    String correlationId
) {}
