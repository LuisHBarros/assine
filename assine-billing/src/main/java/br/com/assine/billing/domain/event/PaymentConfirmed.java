package br.com.assine.billing.domain.event;

import java.util.UUID;

public record PaymentConfirmed(UUID paymentId, UUID subscriptionId) implements DomainEvent {
    @Override
    public String getEventType() {
        return "PaymentConfirmed";
    }
}
