package br.com.assine.billing.domain.event;

import java.util.UUID;

public class ChargebackCreated implements DomainEvent {
    private final UUID chargebackId;
    private final UUID paymentId;
    private final UUID subscriptionId;

    public ChargebackCreated(UUID chargebackId, UUID paymentId, UUID subscriptionId) {
        this.chargebackId = chargebackId;
        this.paymentId = paymentId;
        this.subscriptionId = subscriptionId;
    }

    public UUID getChargebackId() { return chargebackId; }
    public UUID getPaymentId() { return paymentId; }
    public UUID getSubscriptionId() { return subscriptionId; }

    @Override
    public String getEventType() {
        return "ChargebackCreated";
    }
}
