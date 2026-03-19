package br.com.assine.billing.domain.port.out;

import br.com.assine.billing.domain.model.Payment;
import java.util.UUID;

public interface PaymentGateway {
    PaymentIntent createSubscription(CreateSubscriptionCommand command);
    void cancelSubscription(String externalSubscriptionId);
    RefundResult refund(String externalPaymentId, int amountCents);
    ParsedWebhookEvent parseEvent(String payload, String signature);

    record CreateSubscriptionCommand(
        UUID subscriptionId,
        String customerEmail,
        String customerName,
        String paymentMethodId, // e.g., credit card token or 'pix'
        int amountCents
    ) {}

    record PaymentIntent(
        String externalSubscriptionId,
        String externalPaymentId,
        String clientSecret, // For Stripe Elements/Mobile
        String pixQrCode,
        String pixCopyPaste
    ) {}

    record RefundResult(
        String externalRefundId,
        String status // e.g., "succeeded", "pending"
    ) {}

    record ParsedWebhookEvent(
        String eventId,
        WebhookEventType type,
        Payment partialPaymentData,
        String externalRefundId,
        String failReason,
        String externalChargebackId,
        Integer chargebackAmountCents
    ) {}

    enum WebhookEventType {
        PAYMENT_INTENT_CREATED,
        PAYMENT_CONFIRMED,
        PAYMENT_FAILED,
        REFUND_COMPLETED,
        CHARGEBACK_CREATED,
        UNSUPPORTED
    }
}
