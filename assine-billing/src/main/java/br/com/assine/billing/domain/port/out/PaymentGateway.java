package br.com.assine.billing.domain.port.out;

import br.com.assine.billing.domain.model.Payment;

public interface PaymentGateway {
    ParsedWebhookEvent parseEvent(String payload, String signature);

    record ParsedWebhookEvent(String eventId, WebhookEventType type, Payment partialPaymentData, String failReason) {}

    enum WebhookEventType {
        PAYMENT_CONFIRMED,
        PAYMENT_FAILED,
        UNSUPPORTED
    }
}
