package br.com.assine.billing.domain.event;

import java.util.UUID;

public record PaymentFailed(UUID paymentId, UUID subscriptionId, String reason) implements DomainEvent {
    @Override
    public String getEventType() {
        return "PaymentFailed";
    }
}
